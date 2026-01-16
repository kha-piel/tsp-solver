package com.example.tsp.service;

import com.example.tsp.model.AddressData;
import com.example.tsp.model.AddressData.ScheduleInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class SolverService {

    @Data
    @AllArgsConstructor
    public static class TimeWindow {
        private int earliest;
        private int latest;
    }

    public double calculateTotalDistance(List<Integer> pathIndices, double[][] distMatrix) {
        double totalDist = 0;
        for (int i = 0; i < pathIndices.size() - 1; i++) {
            totalDist += distMatrix[pathIndices.get(i)][pathIndices.get(i + 1)];
        }
        return totalDist;
    }

    public List<Integer> runNearestNeighbor(double[][] distMatrix) {
        int numLocations = distMatrix.length;
        List<Integer> pathIndices = new ArrayList<>();
        pathIndices.add(0);
        
        List<Integer> unvisited = new ArrayList<>();
        for (int i = 1; i < numLocations; i++) unvisited.add(i);

        int currentNode = 0;
        while (!unvisited.isEmpty()) {
            int nearestNode = -1;
            double minDist = Double.MAX_VALUE;

            for (Integer node : unvisited) {
                double dist = distMatrix[currentNode][node];
                if (dist != Double.POSITIVE_INFINITY && dist < minDist) {
                    minDist = dist;
                    nearestNode = node;
                }
            }
            // Handling unconnected graph
            if (nearestNode == -1) throw new RuntimeException("Graph not connected");

            currentNode = nearestNode;
            pathIndices.add(currentNode);
            unvisited.remove(Integer.valueOf(currentNode)); // Remove Object, not index
        }
        pathIndices.add(0);
        return pathIndices;
    }

    public List<Integer> apply2Opt(List<Integer> pathIndices, double[][] distMatrix) {
        List<Integer> bestPath = new ArrayList<>(pathIndices);
        boolean improved = true;
        while (improved) {
            improved = false;
            for (int i = 1; i < bestPath.size() - 2; i++) {
                for (int j = i + 1; j < bestPath.size(); j++) {
                     if (j == i + 1) continue;
                     
                     // Distance check
                     double distAC = distMatrix[bestPath.get(i-1)][bestPath.get(i)];
                     double distBD = distMatrix[bestPath.get(j-1)][bestPath.get(j)];
                     double distCurrent = distAC + distBD;

                     double distAD = distMatrix[bestPath.get(i-1)][bestPath.get(j-1)];
                     double distBC = distMatrix[bestPath.get(i)][bestPath.get(j)];
                     double distNew = distAD + distBC;

                     if (distNew < distCurrent) {
                         // Reverse segment i to j-1
                         Collections.reverse(bestPath.subList(i, j));
                         improved = true;
                     }
                }
            }
        }
        return bestPath;
    }

    public List<Integer> run3Opt(double[][] distMatrix) {
        List<Integer> initialPath = runNearestNeighbor(distMatrix);
        if (initialPath.size() < 6) return initialPath;

        List<Integer> bestPath = new ArrayList<>(initialPath);
        boolean improved = true;
        while (improved) {
            improved = false;
            // Simplified 3-opt implementation loop structure based on existing logic
             for (int i = 1; i < bestPath.size() - 4; i++) {
                for (int j = i + 2; j < bestPath.size() - 2; j++) {
                    for (int k = j + 2; k < bestPath.size(); k++) {
                        if (k == bestPath.size() - 1) continue;
                        
                        // Indices logic A..B...C..D...E..F
                        int A = bestPath.get(i-1), B = bestPath.get(i);
                        int C = bestPath.get(j-1), D = bestPath.get(j);
                        int E = bestPath.get(k-1), F = bestPath.get(k);
                        
                        double d0 = distMatrix[A][B] + distMatrix[C][D] + distMatrix[E][F];
                        double d1 = distMatrix[A][D] + distMatrix[E][B] + distMatrix[C][F];
                        
                        if (d1 < d0) {
                            // Reconnect: A->D..C->B..E->F (reverse middle two segments logic)
                            // Python: new_path = best_path[:i] + best_path[j:k] + list(reversed(best_path[i:j])) + best_path[k:]
                            // Note: Python slice[x:y] is exclusive at y.
                            // Java subList is also exclusive at y.
                            
                             List<Integer> newPath = new ArrayList<>();
                             newPath.addAll(bestPath.subList(0, i));
                             newPath.addAll(bestPath.subList(j, k));
                             
                             List<Integer> midReversed = new ArrayList<>(bestPath.subList(i, j));
                             Collections.reverse(midReversed);
                             newPath.addAll(midReversed);
                             
                             newPath.addAll(bestPath.subList(k, bestPath.size()));
                             
                             bestPath = newPath;
                             improved = true;
                             break;
                        }
                    }
                    if (improved) break;
                }
                if (improved) break;
            }
        }
        return bestPath;
    }

    public List<Integer> runSaSolver(double[][] distMatrix) {
        int numLocations = distMatrix.length;
        if (numLocations < 3) return runNearestNeighbor(distMatrix);

        List<Integer> currentSolution = new ArrayList<>();
        for (int i = 1; i < numLocations; i++) currentSolution.add(i);
        Collections.shuffle(currentSolution);
        
        // Add start/end
        currentSolution.add(0, 0); // start
        currentSolution.add(0);    // end

        double currentCost = calculateTotalDistance(currentSolution, distMatrix);
        List<Integer> bestSolution = new ArrayList<>(currentSolution);
        double bestCost = currentCost;

        double temp = 10000;
        double stoppingTemp = 1;
        double alpha = 0.995;
        Random rand = new Random();

        while (temp > stoppingTemp) {
            // Swap two random nodes (excluding start/end) -> indices 1 to n-2 (size-2 is last valid index before end)
            // numLocations total nodes. Nodes are 0 to n-1. Main path has n+1 elements (0...0).
            // Indices to swap: 1 to (size - 2).
            int bound = currentSolution.size() - 2;
            if (bound < 1) break; // Should not happen if check numLocations < 3 is correct.
            
            int i = rand.nextInt(bound) + 1;
            int j = rand.nextInt(bound) + 1;
            while (i == j) {
                 j = rand.nextInt(bound) + 1;
            }

            List<Integer> neighborSolution = new ArrayList<>(currentSolution);
            Collections.swap(neighborSolution, i, j);
            
            double neighborCost = calculateTotalDistance(neighborSolution, distMatrix);
            double costDiff = neighborCost - currentCost;
            
            if (costDiff < 0 || rand.nextDouble() < Math.exp(-costDiff / temp)) {
                currentSolution = new ArrayList<>(neighborSolution);
                currentCost = neighborCost;
                if (currentCost < bestCost) {
                    bestSolution = new ArrayList<>(currentSolution);
                    bestCost = currentCost;
                }
            }
            temp *= alpha;
        }
        return bestSolution;
    }
    
    // TSPTW Logic
    
    public static class TSPTWResult {
        public List<Integer> path;
        public double distance;
        public double cost; // total duration
        public List<ScheduleInfo> schedule;
        
        public TSPTWResult(List<Integer> path, double distance, double cost, List<ScheduleInfo> schedule) {
            this.path = path;
            this.distance = distance;
            this.cost = cost;
            this.schedule = schedule;
        }
    }
    
    public TSPTWResult calculateTsptwCost(List<Integer> pathIndices, double[][] durationMatrix, List<TimeWindow> timeWindows, int startTimeSec) {
        double currentTime = startTimeSec;
        List<ScheduleInfo> schedule = new ArrayList<>();
        
        for (int i = 0; i < pathIndices.size() - 1; i++) {
            int fromNode = pathIndices.get(i);
            int toNode = pathIndices.get(i+1);
            
            double arrivalTime = currentTime + durationMatrix[fromNode][toNode];
            
            double earliest = 0;
            double latest = Double.POSITIVE_INFINITY;
            
            if (toNode != 0) {
                // timeWindows are for nodes 1...N. index = node-1
                TimeWindow tw = timeWindows.get(toNode - 1);
                earliest = tw.getEarliest();
                latest = tw.getLatest();
            }
            
            if (arrivalTime > latest) return new TSPTWResult(null, 0, Double.POSITIVE_INFINITY, null);
            
            double waitTime = Math.max(0, earliest - arrivalTime);
            double departureTime = arrivalTime + waitTime;
            
            schedule.add(new ScheduleInfo(
                    formatTime((int)arrivalTime),
                    formatTime((int)waitTime),
                    formatTime((int)departureTime)
            ));
            
            currentTime = departureTime;
        }
        
        return new TSPTWResult(pathIndices, 0, currentTime - startTimeSec, schedule);
    }
    
    private String formatTime(int totalSeconds) {
        int h = (totalSeconds / 3600) % 24;
        int m = (totalSeconds % 3600) / 60;
        int s = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }
    
    public int timeStrToSeconds(String timeStr) {
        String[] parts = timeStr.split(":");
        return Integer.parseInt(parts[0]) * 3600 + Integer.parseInt(parts[1]) * 60;
    }
    
    public TSPTWResult runSaSolverForTsptw(double[][] distMatrix, double[][] durationMatrix, List<TimeWindow> timeWindows, int startTimeSec) {
        int numLocations = distMatrix.length;
        Random rand = new Random();
        
        if (numLocations < 3) {
             List<Integer> path = new ArrayList<>(Arrays.asList(0, 1, 0));
             TSPTWResult costRes = calculateTsptwCost(path, durationMatrix, timeWindows, startTimeSec);
             costRes.distance = calculateTotalDistance(path, distMatrix);
             return costRes;
        }
        
        List<Integer> currentSolution = new ArrayList<>();
        currentSolution.add(0);
        List<Integer> middle = new ArrayList<>();
        for (int i = 1; i < numLocations; i++) middle.add(i);
        Collections.shuffle(middle); // Initial random shuffle
        currentSolution.addAll(middle);
        currentSolution.add(0);

        TSPTWResult currentRes = calculateTsptwCost(currentSolution, durationMatrix, timeWindows, startTimeSec);
        double currentCost = currentRes.cost;
        
        List<Integer> bestSolution = new ArrayList<>(currentSolution);
        double bestCost = currentCost;
        List<ScheduleInfo> bestSchedule = currentRes.schedule;

        double temp = 10000;
        double stoppingTemp = 1;
        double alpha = 0.995;

        while (temp > stoppingTemp) {
             int bound = currentSolution.size() - 2;
             int i = rand.nextInt(bound) + 1;
             int j = rand.nextInt(bound) + 1;
             
             List<Integer> neighborSolution = new ArrayList<>(currentSolution);
             Collections.swap(neighborSolution, i, j);
             
             TSPTWResult neighborRes = calculateTsptwCost(neighborSolution, durationMatrix, timeWindows, startTimeSec);
             double neighborCost = neighborRes.cost;

             if (neighborCost != Double.POSITIVE_INFINITY) {
                 double costDiff = neighborCost - currentCost;
                 if (costDiff < 0 || rand.nextDouble() < Math.exp(-costDiff / temp)) {
                     currentSolution = neighborSolution;
                     currentCost = neighborCost;
                     
                     if (currentCost < bestCost) {
                         bestSolution = neighborSolution;
                         bestCost = currentCost;
                         bestSchedule = neighborRes.schedule;
                     }
                 }
             }
             temp *= alpha;
        }
        
        if (bestCost == Double.POSITIVE_INFINITY) {
            throw new RuntimeException("Cannot find valid route with given time windows.");
        }
        
        return new TSPTWResult(bestSolution, calculateTotalDistance(bestSolution, distMatrix), bestCost, bestSchedule);
    }
}
