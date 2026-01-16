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
        for (int i = 1; i < numLocations; i++)
            unvisited.add(i);

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
            if (nearestNode == -1)
                throw new RuntimeException("Graph not connected");

            currentNode = nearestNode;
            pathIndices.add(currentNode);
            unvisited.remove(Integer.valueOf(currentNode));
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
                    if (j == i + 1)
                        continue;

                    // Distance check
                    double distAC = distMatrix[bestPath.get(i - 1)][bestPath.get(i)];
                    double distBD = distMatrix[bestPath.get(j - 1)][bestPath.get(j)];
                    double distCurrent = distAC + distBD;

                    double distAD = distMatrix[bestPath.get(i - 1)][bestPath.get(j - 1)];
                    double distBC = distMatrix[bestPath.get(i)][bestPath.get(j)];
                    double distNew = distAD + distBC;

                    if (distNew < distCurrent) {
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
        if (initialPath.size() < 6)
            return initialPath;

        List<Integer> bestPath = new ArrayList<>(initialPath);
        boolean improved = true;
        while (improved) {
            improved = false;
            for (int i = 1; i < bestPath.size() - 4; i++) {
                for (int j = i + 2; j < bestPath.size() - 2; j++) {
                    for (int k = j + 2; k < bestPath.size(); k++) {
                        if (k == bestPath.size() - 1)
                            continue;

                        // Indices logic A..B...C..D...E..F
                        int A = bestPath.get(i - 1), B = bestPath.get(i);
                        int C = bestPath.get(j - 1), D = bestPath.get(j);
                        int E = bestPath.get(k - 1), F = bestPath.get(k);

                        double d0 = distMatrix[A][B] + distMatrix[C][D] + distMatrix[E][F];
                        double d1 = distMatrix[A][D] + distMatrix[E][B] + distMatrix[C][F];

                        if (d1 < d0) {

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
                    if (improved)
                        break;
                }
                if (improved)
                    break;
            }
        }
        return bestPath;
    }

    public List<Integer> runSaSolver(double[][] distMatrix) {
        int numLocations = distMatrix.length;
        if (numLocations < 3)
            return runNearestNeighbor(distMatrix);

        List<Integer> currentSolution = new ArrayList<>();
        for (int i = 1; i < numLocations; i++)
            currentSolution.add(i);
        Collections.shuffle(currentSolution);

        // Add start/end
        currentSolution.add(0, 0); // start
        currentSolution.add(0); // end

        double currentCost = calculateTotalDistance(currentSolution, distMatrix);
        List<Integer> bestSolution = new ArrayList<>(currentSolution);
        double bestCost = currentCost;

        double temp = 10000;
        double stoppingTemp = 1;
        double alpha = 0.995;
        Random rand = new Random();

        while (temp > stoppingTemp) {
            int bound = currentSolution.size() - 2;
            if (bound < 1)
                break;

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

    public static class TSPTWResult {
        public List<Integer> path;
        public double distance;
        public double cost;
        public List<ScheduleInfo> schedule;

        public TSPTWResult(List<Integer> path, double distance, double cost, List<ScheduleInfo> schedule) {
            this.path = path;
            this.distance = distance;
            this.cost = cost;
            this.schedule = schedule;
        }
    }

    public TSPTWResult calculateTsptwCost(List<Integer> pathIndices, double[][] durationMatrix,
            List<TimeWindow> timeWindows, int startTimeSec) {
        double currentTime = startTimeSec;
        List<ScheduleInfo> schedule = new ArrayList<>();

        for (int i = 0; i < pathIndices.size() - 1; i++) {
            int fromNode = pathIndices.get(i);
            int toNode = pathIndices.get(i + 1);

            double arrivalTime = currentTime + durationMatrix[fromNode][toNode];

            double earliest = 0;
            double latest = Double.POSITIVE_INFINITY;

            if (toNode != 0) {
                TimeWindow tw = timeWindows.get(toNode - 1);
                earliest = tw.getEarliest();
                latest = tw.getLatest();
            }

            if (arrivalTime > latest)
                return new TSPTWResult(null, 0, Double.POSITIVE_INFINITY, null);

            double waitTime = Math.max(0, earliest - arrivalTime);
            double departureTime = arrivalTime + waitTime;

            schedule.add(new ScheduleInfo(
                    formatTime((int) arrivalTime),
                    formatTime((int) waitTime),
                    formatTime((int) departureTime)));

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

    public TSPTWResult runSaSolverForTsptw(double[][] distMatrix, double[][] durationMatrix,
            List<TimeWindow> timeWindows, int startTimeSec) {
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
        for (int i = 1; i < numLocations; i++)
            middle.add(i);
        Collections.shuffle(middle);
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

    // A* Search Implementation

    @Data
    @AllArgsConstructor
    private static class AStarNode implements Comparable<AStarNode> {
        int currentCity;
        int visitedMask;
        double gCost; // Cost from start
        double hCost; // Heuristic (MST of unvisited)
        double fCost; // g + h
        AStarNode parent;

        @Override
        public int compareTo(AStarNode other) {
            return Double.compare(this.fCost, other.fCost);
        }
    }

    public List<Integer> runAStarSolver(double[][] distMatrix) {
        int numLocations = distMatrix.length;
        // Limit for A* because it's exact and slow O(n^2 * 2^n)
        if (numLocations > 15) {
            // Fallback to SA or others for large inputs if requested via A*
            return runSaSolver(distMatrix);
        }

        PriorityQueue<AStarNode> openSet = new PriorityQueue<>();
        // Visited states: key = "currentCity_visitedMask", value = gCost
        // If we reach same state with higher gCost, skip.
        Map<String, Double> visitedStates = new HashMap<>();

        // Start at 0
        int startMask = 1; // 0th bit set
        AStarNode startNode = new AStarNode(0, startMask, 0, 0, 0, null);
        startNode.setHCost(calculateMSTHeuristic(startMask, numLocations, distMatrix));
        startNode.setFCost(startNode.getGCost() + startNode.getHCost());

        openSet.add(startNode);
        visitedStates.put(0 + "_" + startMask, 0.0);

        while (!openSet.isEmpty()) {
            AStarNode current = openSet.poll();

            // Goal check: All cities visited and returned to start
            if (current.visitedMask == (1 << numLocations) - 1) {
                if (current.currentCity == 0) {
                    return reconstructPath(current);
                } else {
                    // Since we need to return to start, the only valid next move from full mask is
                    // to 0.
                    // But strictly speaking, TSP cycle visits every city exactly once.
                    // If we are at the last unvisited city, mask becomes full.
                    // Then we take edge back to 0.
                    double distToStart = distMatrix[current.currentCity][0];
                    double totalCost = current.gCost + distToStart;
                    AStarNode endNode = new AStarNode(0, current.visitedMask, totalCost, 0, totalCost, current);
                    return reconstructPath(endNode);
                }
            }

            // Expand neighbors
            boolean allVisited = (current.visitedMask == ((1 << numLocations) - 1));

            if (allVisited && current.currentCity != 0) {
                // Must return to 0
                double distToStart = distMatrix[current.currentCity][0];
                double newGCost = current.gCost + distToStart;
                AStarNode endNode = new AStarNode(0, current.visitedMask, newGCost, 0, newGCost, current);

                String stateKey = "0_" + current.visitedMask;
                if (newGCost < visitedStates.getOrDefault(stateKey, Double.MAX_VALUE)) {
                    openSet.add(endNode);
                    visitedStates.put(stateKey, newGCost);
                }
            } else if (!allVisited) {
                for (int nextCity = 0; nextCity < numLocations; nextCity++) {
                    if ((current.visitedMask & (1 << nextCity)) == 0) { // Not visited yet
                        double newGCost = current.gCost + distMatrix[current.currentCity][nextCity];
                        int newMask = current.visitedMask | (1 << nextCity);

                        String stateKey = nextCity + "_" + newMask;
                        if (visitedStates.containsKey(stateKey) && visitedStates.get(stateKey) <= newGCost) {
                            continue;
                        }

                        double hCost = calculateMSTHeuristic(newMask, numLocations, distMatrix);
                        AStarNode nextNode = new AStarNode(nextCity, newMask, newGCost, hCost, newGCost + hCost,
                                current);
                        openSet.add(nextNode);
                        visitedStates.put(stateKey, newGCost);
                    }
                }
            }
        }

        return new ArrayList<>(); // Should not happen
    }

    private List<Integer> reconstructPath(AStarNode node) {
        List<Integer> path = new ArrayList<>();
        while (node != null) {
            path.add(node.currentCity);
            node = node.parent;
        }
        Collections.reverse(path);
        // Ensure path ends with 0 if it doesn't (though reconstructFromLastNode usually
        // handles it)
        // If logic creates 0 -> ... -> 0, it's correct.
        return path;
    }

    private double calculateMSTHeuristic(int visitedMask, int numLocations, double[][] distMatrix) {
        List<Integer> unvisited = new ArrayList<>();
        for (int i = 0; i < numLocations; i++) {
            if ((visitedMask & (1 << i)) == 0) {
                unvisited.add(i);
            }
        }

        if (unvisited.isEmpty())
            return 0;

        // Prim's MST for unvisited
        double mstCost = 0;
        if (unvisited.size() > 1) {
            Set<Integer> mstIncluded = new HashSet<>();
            double[] minEdge = new double[numLocations];
            Arrays.fill(minEdge, Double.MAX_VALUE);

            int startNode = unvisited.get(0);
            minEdge[startNode] = 0;

            for (int i = 0; i < unvisited.size(); i++) {
                int u = -1;
                double min = Double.MAX_VALUE;
                for (int v : unvisited) {
                    if (!mstIncluded.contains(v) && minEdge[v] < min) {
                        min = minEdge[v];
                        u = v;
                    }
                }

                if (u == -1)
                    break;
                mstIncluded.add(u);
                mstCost += min;

                for (int v : unvisited) {
                    if (!mstIncluded.contains(v) && distMatrix[u][v] < minEdge[v]) {
                        minEdge[v] = distMatrix[u][v];
                    }
                }
            }
        }

        // Heuristic Enhancement: Add cheapest edge from CURRENT city to any in MST
        // and cheapest edge from START (0) to any in MST
        // But currentCity is not available in this method signature easily unless
        // passed cost?
        // Or we just rely on Basic MST. Basic MST is admissible.
        return mstCost;
    }
}
