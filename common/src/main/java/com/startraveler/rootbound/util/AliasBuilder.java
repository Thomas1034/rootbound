/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * If you modify this file, please include a notice stating the changes:
 * Example: "Modified by [Your Name] on [Date] - [Short Description of Changes]"
 */
package com.startraveler.rootbound.util;

import net.minecraft.util.RandomSource;

import java.util.*;
import java.util.function.Function;

/**
 * Alias Method for Weighted Random Selection
 * <p>
 * This class implements the Alias Method, which efficiently selects an item
 * from a set based on weighted probabilities. The Alias Method allows for
 * constant time selection (O(1)) after an initial preprocessing step (O(n)),
 * making it ideal for scenarios with repeated weighted random selections.
 * <p>
 * The method preprocesses the weights into two tables:
 * 1. A probability table containing normalized probabilities for each item.
 * 2. An alias table, which maps items with probabilities greater than 1 to
 * an alias, ensuring the selection process remains efficient.
 * <p>
 * After preprocessing, selecting an item based on its weight is done in O(1) time.
 * <p>
 * Credit: Implementation by ChatGPT (OpenAI).
 */
public class AliasBuilder {

    private AliasBuilder() {
    }

    /**
     * Creates a weighted random selection function using the Alias Method.
     *
     * @param weightedMap A map of item weights, where each key is the weight
     *                    associated with an item (value).
     * @param <T>         The type of the items to be selected.
     * @return A function that takes a Random instance and returns a randomly
     * selected item based on the weights provided in the map.
     */
    public static <T> Function<RandomSource, T> build(Map<T, Integer> weightedMap) {
        // Convert the map entries to a list for easier processing.
        List<Map.Entry<T, Integer>> entries = new ArrayList<>(weightedMap.entrySet());
        int n = entries.size();  // The number of items in the map.

        // Normalize the weights by calculating total weight.
        double totalWeight = entries.stream().mapToDouble(Map.Entry::getValue).sum();

        // Arrays to store the probability of each item and its alias index.
        double[] probabilities = new double[n];
        int[] alias = new int[n];
        T[] objects = (T[]) new Object[n];

        // Two queues to manage items with small and large probabilities.
        Queue<Integer> small = new ArrayDeque<>();
        Queue<Integer> large = new ArrayDeque<>();

        // Step 1: Normalize the weights and classify the items.
        for (int i = 0; i < n; i++) {
            // Calculate the scaled probability for the current item.
            double weight = entries.get(i).getValue() / totalWeight;
            probabilities[i] = weight * n;  // Scaled probability for each item.
            objects[i] = entries.get(i).getKey();  // Store the object.

            // Classify items based on whether their scaled probability is < 1 or >= 1.
            if (probabilities[i] < 1.0) {
                small.add(i);  // Item goes into small set if probability < 1.
            } else {
                large.add(i);  // Item goes into large set if probability >= 1.
            }
        }

        // Step 2: Construct the Alias Table using the small and large queues.
        while (!small.isEmpty() && !large.isEmpty()) {
            // Pop one item from each queue.
            int smallIndex = small.poll();
            int largeIndex = large.poll();

            // The small item gets the large item as its alias.
            alias[smallIndex] = largeIndex;

            // Adjust the probability of the large item by subtracting the small item's probability.
            probabilities[largeIndex] += probabilities[smallIndex] - 1.0;

            // If the large item's new probability is < 1, add it to the small set.
            if (probabilities[largeIndex] < 1.0) {
                small.add(largeIndex);
            } else {
                large.add(largeIndex);
            }
        }

        // Step 3: Ensure that all probabilities are set to 1.0 for items in the small set.
        while (!small.isEmpty()) {
            probabilities[small.poll()] = 1.0;
        }
        while (!large.isEmpty()) {
            probabilities[large.poll()] = 1.0;
        }

        // Step 4: Return a function that uses the alias table for O(1) selection.
        return random -> {
            // Select a random index to choose an item.
            int index = random.nextInt(n);

            // Generate a random number to decide whether to pick the item or its alias.
            if (random.nextDouble() < probabilities[index]) {
                return objects[index];  // Select the item itself.
            } else {
                return objects[alias[index]];  // Select the alias item.
            }
        };
    }
}

