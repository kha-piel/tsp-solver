package com.example.tsp.controller;

import com.example.tsp.model.*;
import com.example.tsp.service.GeocodingService;
import com.example.tsp.service.RoutingService;
import com.example.tsp.service.SolverService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class TspController {

    private final GeocodingService geocodingService;
    private final RoutingService routingService;
    private final SolverService solverService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/")
    public String home(Model model) {
        FormData defaultData = FormData.builder()
                .warehouseAddress("")
                .startTime("08:00")
                .deliveryPoints(new ArrayList<>())
                .mode("distance")
                .build();
        model.addAttribute("form_data", defaultData);
        return "index";
    }

    @PostMapping("/")
    public String solve(@RequestParam Map<String, String> allParams, Model model) {
        String mode = allParams.getOrDefault("mode", "distance");
        String warehouseAddress = allParams.get("warehouse_address");

        List<String> allAddressesText = new ArrayList<>();
        allAddressesText.add(warehouseAddress);

        List<DeliveryPointInput> deliveryPointsInput = new ArrayList<>();
        List<SolverService.TimeWindow> timeWindows = new ArrayList<>();

        // Parse dynamic form keys point_address_X
        List<Integer> indices = allParams.keySet().stream()
                .filter(k -> k.startsWith("point_address_"))
                .map(k -> Integer.parseInt(k.split("_")[2]))
                .sorted()
                .collect(Collectors.toList());

        for (Integer idx : indices) {
            String addr = allParams.get("point_address_" + idx);
            if (addr != null && !addr.isEmpty()) {
                allAddressesText.add(addr);
                DeliveryPointInput dp = new DeliveryPointInput();
                dp.setAddress(addr);

                if ("schedule".equals(mode)) {
                    String e = allParams.getOrDefault("point_earliest_" + idx, "00:00");
                    String l = allParams.getOrDefault("point_latest_" + idx, "23:59");
                    dp.setEarliest(e);
                    dp.setLatest(l);
                    timeWindows.add(new SolverService.TimeWindow(
                            solverService.timeStrToSeconds(e),
                            solverService.timeStrToSeconds(l)));
                }
                deliveryPointsInput.add(dp);
            }
        }

        FormData formData = FormData.builder()
                .warehouseAddress(warehouseAddress)
                .deliveryPoints(deliveryPointsInput)
                .mode(mode)
                .build();

        try {
            if (deliveryPointsInput.isEmpty())
                throw new IllegalArgumentException("Vui lòng nhập ít nhất một điểm giao hàng.");

            List<AddressData> allAddressesData = new ArrayList<>();
            for (String addr : allAddressesText) {
                AddressData ad = geocodingService.getCoordsFromAddress(addr);
                if (ad == null)
                    throw new IllegalArgumentException("Không thể tìm tọa độ cho địa chỉ: " + addr);
                allAddressesData.add(ad);
            }

            RoutingService.RoutingMatrix matrix = routingService.getRouteInfo(allAddressesData);
            if (matrix == null)
                throw new RuntimeException("Không thể lấy dữ liệu từ OSRM API.");

            double[][] distMatrix = matrix.getDistances();
            double[][] durMatrix = matrix.getDurations();

            if ("schedule".equals(mode)) {
                String startTimeStr = allParams.get("start_time");
                formData.setStartTime(startTimeStr);

                SolverService.TSPTWResult result = solverService.runSaSolverForTsptw(
                        distMatrix, durMatrix, timeWindows, solverService.timeStrToSeconds(startTimeStr));

                List<AddressData> finalPath = new ArrayList<>();
                for (int i = 0; i < result.path.size(); i++) {
                    AddressData ad = allAddressesData.get(result.path.get(i));
                    // Clone to avoid reference issues if point visited twice? (TSP usually visits
                    // once, except start/end)
                    // But here start/end are same object in list.
                    // We need to attach schedule info. Ideally return new objects or DTOs.
                    // Simplified: Just use AddressData setter.
                    if (i < result.schedule.size()) {
                        // Note: Schedule size is path len - 1 (segments).
                        // Wait: Logic in Python:
                        // if i < len(final_path) - 1: final_path[i+1]['schedule'] = step
                        // So the schedule info is attached to the DESTINATION node of the segment.

                        // We need to set schedule for node at i+1
                        // Since same object might be in list multiple times (start/end), we MUST clone
                        // or be careful.
                        // Let's create a shallow copy for the view list.
                        AddressData viewNode = new AddressData(ad.getDisplayName(), ad.getLat(), ad.getLon(), null);
                        // The node at i+1 gets the schedule info from schedule[i]
                        // Actually loop in python: for i, step in enumerate(schedule):
                        // final_path[i+1]...
                        // Do this after building list.
                    }
                    finalPath.add(ad);
                }

                // Re-build final path with clones to hold schedule
                List<AddressData> finalPathWithSchedule = new ArrayList<>();
                for (AddressData ad : finalPath) {
                    finalPathWithSchedule.add(new AddressData(ad.getDisplayName(), ad.getLat(), ad.getLon(), null));
                }

                for (int i = 0; i < result.schedule.size(); i++) {
                    if (i + 1 < finalPathWithSchedule.size()) {
                        finalPathWithSchedule.get(i + 1).setSchedule(result.schedule.get(i));
                    }
                }

                model.addAttribute("result_tsptw", finalPathWithSchedule);
                model.addAttribute("distance_km", result.distance / 1000.0);
                model.addAttribute("duration_sec", result.cost);
                model.addAttribute("all_addresses_data", allAddressesData);

            } else {
                List<RouteResult> results = new ArrayList<>();

                // NN + 2-Opt
                long start = System.currentTimeMillis();
                List<Integer> nnPath = solverService.runNearestNeighbor(distMatrix);
                List<Integer> twoOptPath = solverService.apply2Opt(nnPath, distMatrix);
                long end = System.currentTimeMillis();

                results.add(buildResult("NN + 2-Opt", twoOptPath, allAddressesData, distMatrix, end - start));

                // NN + 3-Opt
                start = System.currentTimeMillis();
                List<Integer> threeOptPath = solverService.run3Opt(distMatrix);
                end = System.currentTimeMillis();
                results.add(buildResult("NN + 3-Opt", threeOptPath, allAddressesData, distMatrix, end - start));

                // SA
                start = System.currentTimeMillis();
                List<Integer> saPath = solverService.runSaSolver(distMatrix);
                end = System.currentTimeMillis();
                results.add(buildResult("Simulated Annealing", saPath, allAddressesData, distMatrix, end - start));

                results.sort(Comparator.comparingDouble(RouteResult::getDistanceKm));
                model.addAttribute("results", results);
                model.addAttribute("all_addresses_data", allAddressesData);
            }

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", e.getMessage());
        }

        model.addAttribute("form_data", formData);
        return "index";
    }

    private RouteResult buildResult(String name, List<Integer> indices, List<AddressData> allData,
            double[][] distMatrix, long timeMs) {
        List<AddressData> path = new ArrayList<>();
        for (Integer i : indices)
            path.add(allData.get(i));
        double dist = solverService.calculateTotalDistance(indices, distMatrix);
        return RouteResult.builder()
                .name(name)
                .path(path)
                .distanceKm(dist / 1000.0)
                .execTimeMs(timeMs)
                .build();
    }

    @PostMapping("/reroute")
    @ResponseBody
    public Object reroute(@RequestBody Map<String, Object> payload) {
        try {
            // Need to Deserialize all_addresses_data manually or use DTO.
            // Payload: { all_addresses_data: [...], avoid_segment: {from: x, to: y} }
            List<Map<String, Object>> addrMaps = (List<Map<String, Object>>) payload.get("all_addresses_data");
            Map<String, String> avoidMap = (Map<String, String>) payload.get("avoid_segment");

            if (addrMaps == null || avoidMap == null)
                return Map.of("error", "Dữ liệu không hợp lệ");

            List<AddressData> allAddressesData = new ArrayList<>();
            for (Map<String, Object> m : addrMaps) {
                AddressData ad = new AddressData();
                ad.setDisplayName((String) m.get("display_name"));
                ad.setLat(((Number) m.get("lat")).doubleValue());
                ad.setLon(((Number) m.get("lon")).doubleValue());
                allAddressesData.add(ad);
            }

            RoutingService.RoutingMatrix matrix = routingService.getRouteInfo(allAddressesData);
            if (matrix == null)
                throw new RuntimeException("Connection Error OSRM");

            double[][] distMatrix = matrix.getDistances();

            // Find indices
            int fromIdx = -1, toIdx = -1;
            String fromName = avoidMap.get("from");
            String toName = avoidMap.get("to");

            for (int i = 0; i < allAddressesData.size(); i++) {
                if (allAddressesData.get(i).getDisplayName().equals(fromName))
                    fromIdx = i;
                if (allAddressesData.get(i).getDisplayName().equals(toName))
                    toIdx = i;
            }

            if (fromIdx == -1 || toIdx == -1)
                return Map.of("error", "Address not found");

            distMatrix[fromIdx][toIdx] = Double.POSITIVE_INFINITY;

            List<Integer> initial = solverService.runNearestNeighbor(distMatrix);
            List<Integer> finalPathIdx = solverService.apply2Opt(initial, distMatrix);

            double finalDist = solverService.calculateTotalDistance(finalPathIdx, distMatrix);
            if (Double.isInfinite(finalDist))
                throw new RuntimeException("No valid route found.");

            double totalDur = 0;
            for (int i = 0; i < finalPathIdx.size() - 1; i++) {
                totalDur += matrix.getDurations()[finalPathIdx.get(i)][finalPathIdx.get(i + 1)];
            }

            List<AddressData> finalPath = new ArrayList<>();
            for (int i : finalPathIdx)
                finalPath.add(allAddressesData.get(i));

            RouteResult res = new RouteResult();
            res.setName("Tuyến đường thay thế");
            res.setPath(finalPath);
            res.setDistanceKm(finalDist / 1000.0);

            int h = (int) (totalDur / 3600);
            int m = (int) ((totalDur % 3600) / 60);
            res.setTotalDurationText(String.format("%02d giờ %02d phút", h, m));

            return res;

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("error", e.getMessage());
        }
    }
}
