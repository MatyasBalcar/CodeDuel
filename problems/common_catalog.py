"""Familiar algorithm problems with deterministic cases and independent Python answers."""
from collections import Counter, deque
from functools import lru_cache
from math import gcd
import random


RNG = random.Random(20261026)


def array_input(values):
    return f'{len(values)}\n' + ' '.join(map(str, values)) + '\n'


def two_sum(values, target):
    for first in range(len(values)):
        for second in range(first + 1, len(values)):
            if values[first] + values[second] == target:
                return f'{first + 1} {second + 1}'
    return '-1'


def maximum_subarray(values):
    best = values[0]
    for start in range(len(values)):
        total = 0
        for end in range(start, len(values)):
            total += values[end]
            best = max(best, total)
    return best


def next_permutation(values):
    if len(values) < 2:
        return values.copy()
    best = None

    def search(prefix, remaining):
        nonlocal best
        if not remaining:
            if prefix > values and (best is None or prefix < best):
                best = prefix
            return
        for index, value in enumerate(remaining):
            if index > 0 and value == remaining[index - 1]:
                continue
            search(prefix + [value], remaining[:index] + remaining[index + 1:])

    search([], sorted(values))
    return sorted(values) if best is None else best


def can_jump(values):
    reachable = {0}
    for index, distance in enumerate(values):
        if index in reachable:
            reachable.update(range(index + 1, min(len(values), index + distance + 1)))
    return len(values) - 1 in reachable


def house_robber(values):
    @lru_cache(None)
    def choose(index):
        if index >= len(values):
            return 0
        return max(choose(index + 1), values[index] + choose(index + 2))

    return choose(0)


def word_break(text, words):
    pending = deque([0])
    seen = {0}
    while pending:
        start = pending.popleft()
        if start == len(text):
            return True
        for word in words:
            end = start + len(word)
            if end not in seen and text.startswith(word, start):
                seen.add(end)
                pending.append(end)
    return False


def edit_distance(first, second):
    @lru_cache(None)
    def distance(left, right):
        if left == len(first):
            return len(second) - right
        if right == len(second):
            return len(first) - left
        if first[left] == second[right]:
            return distance(left + 1, right + 1)
        return 1 + min(distance(left + 1, right), distance(left, right + 1),
                       distance(left + 1, right + 1))

    return distance(0, 0)


def longest_common_subsequence(first, second):
    @lru_cache(None)
    def length(left, right):
        if left == len(first) or right == len(second):
            return 0
        if first[left] == second[right]:
            return 1 + length(left + 1, right + 1)
        return max(length(left + 1, right), length(left, right + 1))

    return length(0, 0)


def shortest_path(count, edges, source, target):
    distances = [float('inf')] * count
    distances[source - 1] = 0
    for _ in range(count - 1):
        changed = False
        for start, end, weight in edges:
            candidate = distances[start - 1] + weight
            if candidate < distances[end - 1]:
                distances[end - 1] = candidate
                changed = True
        if not changed:
            break
    return -1 if distances[target - 1] == float('inf') else distances[target - 1]


def course_schedule(count, edges):
    reachable = [[False] * count for _ in range(count)]
    for start, end in edges:
        reachable[start - 1][end - 1] = True
    for middle in range(count):
        for start in range(count):
            for end in range(count):
                reachable[start][end] |= reachable[start][middle] and reachable[middle][end]
    return not any(reachable[index][index] for index in range(count))


def rain_water(heights):
    return sum(min(max(heights[:index + 1]), max(heights[index:])) - height
               for index, height in enumerate(heights))


def histogram_area(heights):
    best = 0
    for start in range(len(heights)):
        lowest = heights[start]
        for end in range(start, len(heights)):
            lowest = min(lowest, heights[end])
            best = max(best, lowest * (end - start + 1))
    return best


def valid_parentheses(text):
    best = 0
    for start in range(len(text)):
        balance = 0
        for end in range(start, len(text)):
            balance += 1 if text[end] == '(' else -1
            if balance < 0:
                break
            if balance == 0:
                best = max(best, end - start + 1)
    return best


def minimum_spanning_tree(count, edges):
    visited = {1}
    total = 0
    while len(visited) < count:
        candidates = []
        for first, second, weight in edges:
            if (first in visited) != (second in visited):
                candidates.append((weight, second if first in visited else first))
        if not candidates:
            return 'IMPOSSIBLE'
        weight, vertex = min(candidates)
        total += weight
        visited.add(vertex)
    return total


def knapsack(items, capacity):
    best = 0
    for chosen in range(1 << len(items)):
        weight = sum(item[0] for index, item in enumerate(items) if chosen & (1 << index))
        if weight <= capacity:
            value = sum(item[1] for index, item in enumerate(items) if chosen & (1 << index))
            best = max(best, value)
    return best


def build_common(problem, case):
    result = []

    def add(slug, title, difficulty, tags, statement, constraints, input_format, output_format, tests):
        content = problem(slug, title, difficulty, tags, statement, constraints, input_format,
                          output_format, [case(data, str(answer), index < 2)
                                          for index, (data, answer) in enumerate(tests)])
        content['durationSeconds'] = {'EASY': 600, 'MEDIUM': 900, 'HARD': 1200}[difficulty]
        content['starterCode'] = '''import java.io.*;
import java.util.*;

public class Main {
    public static void main(String[] args) throws Exception {
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
    }
}
'''
        result.append(content)

    pairs = [([2, 7, 11, 15], 9), ([1, 2, 3], 8), ([3, 3], 6),
             ([], 0), ([0, 0, 0], 0), ([1, 4, 2, 3], 5), ([-5, 2, 5, -2], 0),
             ([1000000000, 1000000000], 2000000000)]
    pairs += [([RNG.randint(-20, 20) for _ in range(RNG.randint(0, 35))], RNG.randint(-30, 30))
              for _ in range(12)]
    tests = [(f'{len(values)} {target}\n' + ' '.join(map(str, values)) + '\n', two_sum(values, target))
             for values, target in pairs]
    tests += [('100000 1\n' + '0 ' * 99999 + '1\n', '1 100000')]
    add('two-sum', 'Two Sum', 'EASY', ['Arrays', 'Hash maps'],
        'Find two different positions in an array whose values add to the target. If several pairs work, choose the smallest first position, then the smallest second position. Positions start at 1 and the first position must be smaller than the second.',
        '0 ≤ n ≤ 100,000; each value is between −1,000,000,000 and 1,000,000,000; |target| ≤ 2,000,000,000.',
        'First line: n and target. Second line: n integers, possibly empty.',
        'Print the two positions separated by a space, or -1 if no pair exists.', tests)

    texts = ['racecar', 'hello', '', 'a', 'abba', 'ab', '12321', '00100']
    texts += [''.join(RNG.choices('abcd0123', k=RNG.randint(0, 60))) for _ in range(8)]
    texts += [text + text[::-1] for text in texts[8:12]]
    tests = [(text + '\n', 'YES' if text == text[::-1] else 'NO') for text in texts]
    tests += [('a' * 200000 + '\n', 'YES')]
    add('palindrome-check', 'Palindrome Check', 'EASY', ['Strings', 'Two pointers'],
        'Check whether a string reads the same from left to right and from right to left. Compare every character exactly as written. The empty string is a palindrome.',
        '0 ≤ string length ≤ 200,000. Characters are lowercase English letters or digits.',
        'One line containing the string. The line may be empty.',
        'Print YES for a palindrome, otherwise NO.', tests)

    anagrams = [('listen', 'silent'), ('rat', 'car'), ('', ''), ('a', ''), ('aab', 'abb'), ('abc', 'cba')]
    for _ in range(12):
        first = ''.join(RNG.choices('abcde', k=RNG.randint(0, 40)))
        second = ''.join(RNG.sample(first, len(first))) if RNG.randrange(2) else first + 'a'
        anagrams.append((first, second))
    tests = [(first + '\n' + second + '\n', 'YES' if Counter(first) == Counter(second) else 'NO')
             for first, second in anagrams]
    tests += [('a' * 100000 + 'b' * 100000 + '\n' + 'ba' * 100000 + '\n', 'YES')]
    add('valid-anagram', 'Valid Anagram', 'EASY', ['Strings', 'Counting'],
        'Decide whether the second string can be made by rearranging all characters of the first. Every character must be used exactly once, so repeated letters must have the same counts.',
        'Each string has 0 to 200,000 lowercase English letters.',
        'Two lines, one string on each line. Either line may be empty.',
        'Print YES if the strings are anagrams, otherwise NO.', tests)

    prices = [[7, 1, 5, 3, 6, 4], [7, 6, 4, 3, 1], [], [5], [0, 1000000000], [3, 3, 3]]
    prices += [[RNG.randint(0, 100) for _ in range(RNG.randint(0, 35))] for _ in range(12)]
    tests = [(array_input(values), max([0] + [values[j] - values[i]
             for i in range(len(values)) for j in range(i + 1, len(values))])) for values in prices]
    tests += [(array_input(list(range(100000))), 99999)]
    add('best-stock-profit', 'Best Time to Buy and Sell Stock', 'EASY', ['Arrays', 'Greedy'],
        'An array gives a stock price for each day. You may buy once and sell on a later day. Find the largest possible profit. You may also make no trade, for a profit of zero.',
        '0 ≤ n ≤ 100,000; 0 ≤ price ≤ 1,000,000,000.',
        'First line: n. Second line: n prices in day order.',
        'Print the maximum profit.', tests)

    arrays = [[1, 2, 3, 1], [1, 2, 3, 4], [], [7], [-1, -1], [0, 1000000000, -1000000000]]
    arrays += [[RNG.randint(-20, 20) for _ in range(RNG.randint(0, 50))] for _ in range(12)]
    tests = [(array_input(values), 'YES' if len(set(values)) < len(values) else 'NO') for values in arrays]
    tests += [(array_input(list(range(100000))), 'NO')]
    add('contains-duplicate', 'Contains Duplicate', 'EASY', ['Arrays', 'Hash sets'],
        'Check whether any value appears at least twice in an array. Equal values at different positions count as a duplicate.',
        '0 ≤ n ≤ 100,000; |value| ≤ 1,000,000,000.',
        'First line: n. Second line: n integers.',
        'Print YES if a duplicate exists, otherwise NO.', tests)

    missing = [[3, 0, 1], [0, 1], [], [1], [0], [4, 2, 1, 0]]
    for _ in range(12):
        count = RNG.randint(0, 60)
        absent = RNG.randrange(count + 1)
        values = [value for value in range(count + 1) if value != absent]
        RNG.shuffle(values)
        missing.append(values)
    tests = [(array_input(values), next(iter(set(range(len(values) + 1)) - set(values)))) for values in missing]
    tests += [(array_input(list(range(100000))), 100000)]
    add('missing-number', 'Missing Number', 'EASY', ['Arrays', 'Math'],
        'An array contains n different numbers chosen from 0 through n. Exactly one number in that range is absent. Find it. The array is not necessarily sorted.',
        '0 ≤ n ≤ 100,000. All given numbers are distinct and between 0 and n.',
        'First line: n. Second line: n integers.',
        'Print the missing number.', tests)

    zeros = [[0, 1, 0, 3, 12], [0, 0], [], [1, 2, 3], [0], [2, -1, 0, -2, 0, 4]]
    zeros += [[RNG.randint(-2, 2) for _ in range(RNG.randint(0, 45))] for _ in range(12)]
    tests = [(array_input(values), ' '.join(map(str, [value for value in values if value] + [0] * values.count(0))))
             for values in zeros]
    tests += [(array_input([0, 1] * 10000), ' '.join(map(str, [1] * 10000 + [0] * 10000)))]
    add('move-zeroes', 'Move Zeroes', 'EASY', ['Arrays', 'Two pointers'],
        'Move every zero to the end of the array while keeping the original order of all nonzero values. Print the resulting array, including every zero.',
        '0 ≤ n ≤ 100,000; |value| ≤ 1,000,000,000.',
        'First line: n. Second line: n integers.',
        'Print the resulting values separated by spaces. If n is zero, print an empty line.', tests)

    singles = [[2, 2, 1], [4, 1, 2, 1, 2], [0], [-7, 2, 2], [1000000000, -1, -1]]
    for _ in range(12):
        values = RNG.sample(range(-100, 101), RNG.randint(1, 30))
        items = values[1:] * 2 + values[:1]
        RNG.shuffle(items)
        singles.append(items)
    tests = [(array_input(values), next(value for value, count in Counter(values).items() if count == 1))
             for values in singles]
    tests += [(array_input(list(range(49999)) * 2 + [-1]), -1)]
    add('single-number', 'Single Number', 'EASY', ['Arrays', 'Bit manipulation'],
        'Exactly one array value appears once. Every other distinct value appears exactly twice. Find the value that appears once. Aim for linear time and constant extra space.',
        '1 ≤ n ≤ 99,999; n is odd; |value| ≤ 1,000,000,000. The stated occurrence counts are guaranteed.',
        'First line: n. Second line: n integers.',
        'Print the value that occurs once.', tests)

    majorities = [[3, 2, 3], [2, 2, 1, 1, 1, 2, 2], [7], [-1, -1, 2], [0, 0]]
    for _ in range(12):
        count = RNG.randint(1, 60)
        majority = RNG.randint(-5, 5)
        values = [majority] * (count // 2 + 1) + [RNG.randint(-5, 5) for _ in range(count - count // 2 - 1)]
        RNG.shuffle(values)
        majorities.append(values)
    tests = [(array_input(values), Counter(values).most_common(1)[0][0]) for values in majorities]
    tests += [(array_input([5] * 50001 + [2] * 49999), 5)]
    add('majority-element', 'Majority Element', 'EASY', ['Arrays', 'Counting'],
        'Find the value that appears in more than half of the array positions. Such a value is guaranteed to exist. Aim for linear time and constant extra space.',
        '1 ≤ n ≤ 100,000; |value| ≤ 1,000,000,000. One value occurs more than n / 2 times.',
        'First line: n. Second line: n integers.',
        'Print the majority value.', tests)

    ranges = [([-2, 0, 3, -5, 2, -1], [(1, 3), (3, 6), (1, 6)]), ([5], [(1, 1)]),
              ([1000000000] * 4, [(1, 4), (2, 2)]), ([0, 0, 0], [(1, 3)])]
    for _ in range(12):
        values = [RNG.randint(-100, 100) for _ in range(RNG.randint(1, 50))]
        queries = [sorted(RNG.choices(range(1, len(values) + 1), k=2)) for _ in range(15)]
        ranges.append((values, queries))
    tests = [(f'{len(values)} {len(queries)}\n' + ' '.join(map(str, values)) + '\n'
              + ''.join(f'{left} {right}\n' for left, right in queries),
              '\n'.join(str(sum(values[left - 1:right])) for left, right in queries))
             for values, queries in ranges]
    tests += [('100000 2\n' + '1000000000 ' * 100000 + '\n1 100000\n50000 50000\n', '100000000000000\n1000000000')]
    add('range-sum-query', 'Range Sum Query', 'EASY', ['Arrays', 'Prefix sums'],
        'Answer sum queries on an array that never changes. Each query names an inclusive range of positions. Positions start at 1. Preprocess the array so that each query can be answered quickly.',
        '1 ≤ n, q ≤ 100,000; |value| ≤ 1,000,000,000; 1 ≤ left ≤ right ≤ n. Sums may need 64 bits.',
        'First line: n and q. Second line: n integers. Then q lines containing left and right.',
        'Print one range sum per line, in query order.', tests)

    numbers = [(12, 18), (7, 13), (1, 1), (1000000000, 999999999), (1000000000, 1000000000)]
    numbers += [(RNG.randint(1, 1000000000), RNG.randint(1, 1000000000)) for _ in range(12)]
    add('gcd-and-lcm', 'GCD and LCM', 'EASY', ['Math', 'Number theory'],
        'Find the greatest common divisor and least common multiple of two positive integers. The greatest common divisor divides both numbers; the least common multiple is the smallest positive number divisible by both.',
        '1 ≤ a, b ≤ 1,000,000,000. The least common multiple may need a 64-bit integer.',
        'One line containing a and b.',
        'Print the greatest common divisor, then the least common multiple, separated by a space.',
        [(f'{first} {second}\n', f'{gcd(first, second)} {first // gcd(first, second) * second}')
         for first, second in numbers])

    merges = [([1, 2, 4], [1, 3, 4]), ([], [0]), ([], []), ([-4, -1], [-3, 0, 2]), ([1, 1], [])]
    merges += [(sorted(RNG.randint(-30, 30) for _ in range(RNG.randint(0, 40))),
                sorted(RNG.randint(-30, 30) for _ in range(RNG.randint(0, 40)))) for _ in range(12)]
    tests = [(f'{len(first)} {len(second)}\n' + ' '.join(map(str, first)) + '\n'
              + ' '.join(map(str, second)) + '\n', ' '.join(map(str, sorted(first + second))))
             for first, second in merges]
    tests += [('10000 10000\n' + '1 ' * 10000 + '\n' + '0 ' * 10000 + '\n',
               ' '.join(map(str, [0] * 10000 + [1] * 10000)))]
    add('merge-sorted-arrays', 'Merge Sorted Arrays', 'EASY', ['Arrays', 'Two pointers'],
        'Merge two arrays that are already sorted in nondecreasing order. Keep all values, including duplicates, and print them in nondecreasing order. Aim for linear time in the combined length.',
        '0 ≤ n, m ≤ 100,000; |value| ≤ 1,000,000,000. Both input arrays are sorted.',
        'First line: n and m. Second line: n values. Third line: m values. Either array line may be empty.',
        'Print the merged array on one line, or an empty line when both arrays are empty.', tests)

    subarrays = [[-2, 1, -3, 4, -1, 2, 1, -5, 4], [-3, -2, -5], [0], [1000000000] * 4, [1, -1, 1]]
    subarrays += [[RNG.randint(-20, 20) for _ in range(RNG.randint(1, 50))] for _ in range(12)]
    tests = [(array_input(values), maximum_subarray(values)) for values in subarrays]
    tests += [(array_input([1000000000] * 100000), 100000000000000)]
    add('maximum-subarray', 'Maximum Subarray', 'MEDIUM', ['Arrays', 'Dynamic programming'],
        'Find the largest sum of a nonempty contiguous part of an array. The chosen part must contain at least one value, including when every value is negative.',
        '1 ≤ n ≤ 100,000; |value| ≤ 1,000,000,000. The answer may need 64 bits.',
        'First line: n. Second line: n integers.',
        'Print the maximum subarray sum.', tests)

    permutations = [[1, 2, 3], [3, 2, 1], [], [4], [1, 1, 5], [2, 3, 1], [2, 2, 2]]
    permutations += [[RNG.randint(-2, 2) for _ in range(RNG.randint(1, 7))] for _ in range(12)]
    tests = [(array_input(values), ' '.join(map(str, next_permutation(values)))) for values in permutations]
    tests += [(array_input([1] * 19998 + [2, 3]), ' '.join(map(str, [1] * 19998 + [3, 2])))]
    add('next-permutation', 'Next Permutation', 'MEDIUM', ['Arrays', 'Two pointers'],
        'Rearrange the array into the next lexicographically greater ordering of its values. Compare arrays at the first position where they differ. If no greater ordering exists, return the smallest ordering. Repeated values are allowed.',
        '0 ≤ n ≤ 100,000; |value| ≤ 1,000,000,000. Aim for linear time and constant extra space.',
        'First line: n. Second line: n integers.',
        'Print the next ordering on one line. For an empty array, print an empty line.', tests)

    containers = [[1, 8, 6, 2, 5, 4, 8, 3, 7], [1, 1], [0, 0], [4, 3, 2, 1, 4], [1000000000, 1000000000]]
    containers += [[RNG.randint(0, 30) for _ in range(RNG.randint(2, 40))] for _ in range(12)]
    tests = [(array_input(heights), max((right - left) * min(heights[left], heights[right])
              for left in range(len(heights)) for right in range(left + 1, len(heights)))) for heights in containers]
    tests += [(array_input([1000000000] * 100000), 99999000000000)]
    add('container-with-most-water', 'Container With Most Water', 'MEDIUM', ['Arrays', 'Two pointers'],
        'Vertical lines stand one unit apart. Choose two lines to form the sides of a container. Its width is the distance between their positions and its height is the shorter line. Find the largest possible area. Lines between the chosen sides do not affect the area.',
        '2 ≤ n ≤ 100,000; 0 ≤ height ≤ 1,000,000,000. The area may need 64 bits.',
        'First line: n. Second line: n heights.',
        'Print the maximum container area.', tests)

    temperatures = [[73, 74, 75, 71, 69, 72, 76, 73], [30, 40, 50, 60], [], [5], [4, 4, 4], [5, 4, 3, 6]]
    temperatures += [[RNG.randint(-50, 100) for _ in range(RNG.randint(0, 50))] for _ in range(12)]
    tests = []
    for values in temperatures:
        answers = [next((later - index for later in range(index + 1, len(values)) if values[later] > value), 0)
                   for index, value in enumerate(values)]
        tests.append((array_input(values), ' '.join(map(str, answers))))
    tests += [(array_input([100] * 20000), ' '.join(['0'] * 20000))]
    add('daily-temperatures', 'Daily Temperatures', 'MEDIUM', ['Arrays', 'Monotonic stacks'],
        'For each day, find how many days you must wait for a strictly warmer temperature. Equal temperatures do not count as warmer. If there is no later warmer day, use zero.',
        '0 ≤ n ≤ 100,000; −50 ≤ temperature ≤ 100. Aim for linear time.',
        'First line: n. Second line: n temperatures in day order.',
        'Print n waiting times on one line. For no days, print an empty line.', tests)

    searches = [([4, 5, 6, 7, 0, 1, 2], 0), ([4, 5, 6, 7, 0, 1, 2], 3), ([], 4), ([5], 5), ([5], 2)]
    for _ in range(16):
        values = sorted(RNG.sample(range(-100, 101), RNG.randint(1, 45)))
        pivot = RNG.randrange(len(values))
        values = values[pivot:] + values[:pivot]
        target = RNG.choice(values) if RNG.randrange(2) else 101
        searches.append((values, target))
    tests = [(f'{len(values)} {target}\n' + ' '.join(map(str, values)) + '\n',
              values.index(target) if target in values else -1) for values, target in searches]
    values = list(range(50000, 100000)) + list(range(50000))
    tests += [(f'100000 49999\n' + ' '.join(map(str, values)) + '\n', 99999)]
    add('search-rotated-array', 'Search in Rotated Sorted Array', 'MEDIUM', ['Arrays', 'Binary search'],
        'A strictly increasing array was rotated by moving a possibly empty prefix to the end. Find the target in the rotated array. All values are distinct. Aim for logarithmic search time.',
        '0 ≤ n ≤ 100,000; values and target are between −1,000,000,000 and 1,000,000,000.',
        'First line: n and target. Second line: the n rotated values.',
        'Print the zero-based position of the target, or -1 when absent.', tests)

    jumps = [[2, 3, 1, 1, 4], [3, 2, 1, 0, 4], [0], [1, 0], [0, 2], [1000000000, 0, 0]]
    jumps += [[RNG.randint(0, 6) for _ in range(RNG.randint(1, 40))] for _ in range(12)]
    tests = [(array_input(values), 'YES' if can_jump(values) else 'NO') for values in jumps]
    tests += [(array_input([1] * 100000), 'YES')]
    add('jump-game', 'Jump Game', 'MEDIUM', ['Arrays', 'Greedy'],
        'Start at the first array position. A value tells you the maximum number of positions you may jump forward from there; shorter positive jumps are allowed. Decide whether you can reach the last position. A one-element array is already at the destination.',
        '1 ≤ n ≤ 100,000; 0 ≤ maximum jump ≤ 1,000,000,000.',
        'First line: n. Second line: n maximum jump lengths.',
        'Print YES if the last position is reachable, otherwise NO.', tests)

    houses = [[1, 2, 3, 1], [2, 7, 9, 3, 1], [], [8], [0, 0, 0], [1000000000] * 4]
    houses += [[RNG.randint(0, 50) for _ in range(RNG.randint(0, 30))] for _ in range(12)]
    tests = [(array_input(values), house_robber(values)) for values in houses]
    tests += [(array_input([1000000000] * 100000), 50000000000000)]
    add('house-robber', 'House Robber', 'MEDIUM', ['Arrays', 'Dynamic programming'],
        'Each house in a straight row contains the given amount of money. Choose houses to rob without choosing two adjacent houses. Find the largest total you can collect. Choosing no houses is allowed.',
        '0 ≤ n ≤ 100,000; 0 ≤ amount ≤ 1,000,000,000. The total may need 64 bits.',
        'First line: n. Second line: n amounts in house order.',
        'Print the maximum total.', tests)

    words = [('leetcode', ['leet', 'code']), ('catsandog', ['cats', 'dog', 'sand', 'and', 'cat']),
             ('', []), ('a', []), ('aaaaaaa', ['aaaa', 'aaa']), ('applepenapple', ['apple', 'pen'])]
    for _ in range(12):
        dictionary = sorted({''.join(RNG.choices('ab', k=RNG.randint(1, 4))) for _ in range(7)})
        text = ''.join(RNG.choices('ab', k=RNG.randint(0, 30)))
        words.append((text, dictionary))
    tests = [(text + f'\n{len(dictionary)}\n' + '\n'.join(dictionary) + '\n',
              'YES' if word_break(text, dictionary) else 'NO') for text, dictionary in words]
    tests += [('a' * 1999 + 'b\n20\n' + '\n'.join('a' * length for length in range(1, 21)) + '\n', 'NO')]
    add('word-break', 'Word Break', 'MEDIUM', ['Strings', 'Dynamic programming'],
        'Decide whether the entire string can be split into one or more dictionary words. You may reuse a word as often as needed. An empty string needs no words and always succeeds.',
        '0 ≤ string length ≤ 2,000; 0 ≤ dictionary size ≤ 500; each word has 1 to 20 lowercase English letters. The string also uses lowercase English letters. Dictionary words are distinct.',
        'First line: the string, possibly empty. Second line: dictionary size. Then one word per line.',
        'Print YES if a complete split is possible, otherwise NO.', tests)

    edits = [('horse', 'ros'), ('intention', 'execution'), ('', ''), ('abc', ''), ('', 'abc'), ('same', 'same')]
    edits += [(''.join(RNG.choices('abc', k=RNG.randint(0, 15))),
               ''.join(RNG.choices('abc', k=RNG.randint(0, 15)))) for _ in range(12)]
    tests = [(first + '\n' + second + '\n', edit_distance(first, second)) for first, second in edits]
    tests += [('a' * 1500 + '\n' + 'b' * 1500 + '\n', 1500)]
    add('edit-distance', 'Edit Distance', 'MEDIUM', ['Strings', 'Dynamic programming'],
        'Turn the first string into the second using as few edits as possible. One edit inserts a character, deletes a character, or replaces one character. Every edit costs one.',
        'Each string has 0 to 1,500 lowercase English letters.',
        'Two lines, one string per line. Either line may be empty.',
        'Print the minimum number of edits.', tests)

    subsequences = [('abcde', 'ace'), ('abc', 'def'), ('', ''), ('abc', ''), ('aaaa', 'aa'), ('abcbdab', 'bdcaba')]
    subsequences += [(''.join(RNG.choices('abcd', k=RNG.randint(0, 20))),
                      ''.join(RNG.choices('abcd', k=RNG.randint(0, 20)))) for _ in range(12)]
    tests = [(first + '\n' + second + '\n', longest_common_subsequence(first, second)) for first, second in subsequences]
    tests += [('ab' * 1000 + '\n' + 'a' * 2000 + '\n', 1000)]
    add('longest-common-subsequence', 'Longest Common Subsequence', 'MEDIUM', ['Strings', 'Dynamic programming'],
        'Find the length of the longest sequence of characters that appears in both strings in the same order. Characters may be skipped, but the remaining characters cannot be rearranged. The empty sequence has length zero.',
        'Each string has 0 to 2,000 lowercase English letters.',
        'Two lines, one string per line. Either line may be empty.',
        'Print the longest common subsequence length.', tests)

    paths = [(4, [(1, 2, 5), (1, 3, 1), (3, 2, 1), (2, 4, 2), (3, 4, 9)], 1, 4),
             (3, [(1, 2, 7)], 1, 3), (1, [], 1, 1), (2, [(1, 2, 0)], 1, 2),
             (3, [(1, 2, 9), (1, 2, 2), (2, 3, 4), (2, 2, 0)], 1, 3)]
    for _ in range(12):
        count = RNG.randint(1, 9)
        edges = [(RNG.randint(1, count), RNG.randint(1, count), RNG.randint(0, 30))
                 for _ in range(RNG.randint(0, 30))]
        paths.append((count, edges, RNG.randint(1, count), RNG.randint(1, count)))
    tests = [(f'{count} {len(edges)} {source} {target}\n' + ''.join(f'{a} {b} {w}\n' for a, b, w in edges),
              shortest_path(count, edges, source, target)) for count, edges, source, target in paths]
    tests += [('50000 49999 1 50000\n' + ''.join(f'{i} {i + 1} 1000000000\n' for i in range(1, 50000)),
               49999000000000)]
    add('shortest-path', 'Shortest Path', 'MEDIUM', ['Graphs', 'Dijkstra', 'Heaps'],
        'Find the cheapest route from the source vertex to the target in a directed weighted graph. Edge weights are nonnegative. You may follow an edge only in its listed direction. Parallel edges and self-loops are allowed.',
        '1 ≤ n ≤ 100,000; 0 ≤ m ≤ 200,000; vertices are numbered 1 through n; 0 ≤ edge weight ≤ 1,000,000,000. Route costs may need 64 bits.',
        'First line: n, m, source, target. Then m lines, each containing from, to, weight.',
        'Print the cheapest route cost, or -1 if unreachable. If source equals target, print 0.', tests)

    courses = [(3, [(1, 2), (2, 3)]), (2, [(1, 2), (2, 1)]), (1, []), (1, [(1, 1)]),
               (4, [(1, 2), (1, 2), (3, 4)])]
    for _ in range(12):
        count = RNG.randint(1, 9)
        edges = [(RNG.randint(1, count), RNG.randint(1, count)) for _ in range(RNG.randint(0, 20))]
        courses.append((count, edges))
    tests = [(f'{count} {len(edges)}\n' + ''.join(f'{a} {b}\n' for a, b in edges),
              'YES' if course_schedule(count, edges) else 'NO') for count, edges in courses]
    tests += [('100000 99999\n' + ''.join(f'{i} {i + 1}\n' for i in range(1, 100000)), 'YES')]
    add('course-schedule', 'Course Schedule', 'MEDIUM', ['Graphs', 'Topological sorting'],
        'You want to finish every course. A prerequisite pair a b means course a must be completed before course b. Decide whether any valid order exists. Separate groups of courses are allowed, but a prerequisite cycle makes completion impossible.',
        '1 ≤ n ≤ 100,000; 0 ≤ m ≤ 200,000; course numbers are 1 through n. Repeated pairs and self-prerequisites may appear.',
        'First line: n and m. Then m prerequisite pairs a b.',
        'Print YES if every course can be completed, otherwise NO.', tests)

    rain = [[0, 1, 0, 2, 1, 0, 1, 3, 2, 1, 2, 1], [4, 2, 0, 3, 2, 5], [], [5], [4, 4], [3, 2, 1]]
    rain += [[RNG.randint(0, 20) for _ in range(RNG.randint(0, 45))] for _ in range(12)]
    tests = [(array_input(heights), rain_water(heights)) for heights in rain]
    tests += [(array_input([1000000000] + [0] * 99998 + [1000000000]), 99998000000000)]
    add('trapping-rain-water', 'Trapping Rain Water', 'HARD', ['Arrays', 'Two pointers'],
        'Bars of width one form a height map. After it rains, water can remain above lower bars when taller bars hold it on both sides. Find the total amount of trapped water. Water can escape through either end.',
        '0 ≤ n ≤ 100,000; 0 ≤ height ≤ 1,000,000,000. The total may need 64 bits.',
        'First line: n. Second line: n heights.',
        'Print the total trapped water.', tests)

    windows = [([1, 3, -1, -3, 5, 3, 6, 7], 3), ([9, 7, 5], 1), ([4], 1), ([2, 2, 2], 2),
               ([-5, -2, -7], 3)]
    for _ in range(12):
        values = [RNG.randint(-30, 30) for _ in range(RNG.randint(1, 50))]
        windows.append((values, RNG.randint(1, len(values))))
    tests = [(f'{len(values)} {size}\n' + ' '.join(map(str, values)) + '\n',
              ' '.join(str(max(values[start:start + size])) for start in range(len(values) - size + 1)))
             for values, size in windows]
    tests += [('100000 99999\n' + ' '.join(map(str, range(100000, 0, -1))) + '\n', '100000 99999')]
    add('sliding-window-maximum', 'Sliding Window Maximum', 'HARD', ['Arrays', 'Monotonic queues'],
        'Slide a window of exactly k consecutive positions across the array, starting at the first position and moving one position at a time. Report the largest value in every window. Aim for linear total time.',
        '1 ≤ k ≤ n ≤ 100,000; |value| ≤ 1,000,000,000.',
        'First line: n and k. Second line: n integers.',
        'Print the window maximums on one line, from left to right.', tests)

    histograms = [[2, 1, 5, 6, 2, 3], [2, 4], [], [0], [5, 5, 5], [1, 2, 3, 4], [4, 3, 2, 1]]
    histograms += [[RNG.randint(0, 20) for _ in range(RNG.randint(0, 40))] for _ in range(12)]
    tests = [(array_input(heights), histogram_area(heights)) for heights in histograms]
    tests += [(array_input([1000000000] * 100000), 100000000000000)]
    add('largest-rectangle-histogram', 'Largest Rectangle in Histogram', 'HARD', ['Arrays', 'Monotonic stacks'],
        'A histogram has adjacent bars of width one. Find the largest rectangle that fits entirely under the bars. The rectangle must have a horizontal top and cover a contiguous range of bars. An empty histogram has area zero.',
        '0 ≤ n ≤ 100,000; 0 ≤ height ≤ 1,000,000,000. The area may need 64 bits.',
        'First line: n. Second line: n heights.',
        'Print the largest rectangle area.', tests)

    parentheses = ['(()', ')()())', '', '(', ')', '()()', '((()))', '))((', '(()())())']
    parentheses += [''.join(RNG.choices('()', k=RNG.randint(0, 80))) for _ in range(12)]
    tests = [(text + '\n', valid_parentheses(text)) for text in parentheses]
    tests += [('(' * 100000 + ')' * 100000 + '\n', 200000)]
    add('longest-valid-parentheses', 'Longest Valid Parentheses', 'HARD', ['Strings', 'Stacks'],
        'Find the length of the longest contiguous part of a parentheses string that is balanced. A balanced part pairs every opening parenthesis with a later closing parenthesis, with groups closing in the proper order.',
        '0 ≤ string length ≤ 200,000. Every character is ( or ).',
        'One line containing the string, possibly empty.',
        'Print the longest balanced substring length, or 0 if no nonempty substring is balanced.', tests)

    trees = [(4, [(1, 2, 1), (2, 3, 2), (3, 4, 3), (1, 4, 10)]),
             (4, [(1, 2, 1), (3, 4, 1)]), (1, []),
             (3, [(1, 1, -10), (1, 2, 8), (1, 2, -2), (2, 3, 0)])]
    for _ in range(12):
        count = RNG.randint(1, 8)
        edges = [(RNG.randint(1, count), RNG.randint(1, count), RNG.randint(-20, 30))
                 for _ in range(RNG.randint(0, 25))]
        trees.append((count, edges))
    tests = [(f'{count} {len(edges)}\n' + ''.join(f'{a} {b} {w}\n' for a, b, w in edges),
              minimum_spanning_tree(count, edges)) for count, edges in trees]
    tests += [('50000 49999\n' + ''.join(f'{i} {i + 1} 1000000000\n' for i in range(1, 50000)),
               49999000000000)]
    add('minimum-spanning-tree', 'Minimum Spanning Tree', 'HARD', ['Graphs', 'Disjoint sets', 'Sorting'],
        'Connect every vertex of an undirected weighted graph using edges with the smallest possible total weight. The chosen edges must form a tree, with no cycles. Negative weights, parallel edges, and self-loops may appear. A self-loop cannot help connect different vertices.',
        '1 ≤ n ≤ 100,000; 0 ≤ m ≤ 200,000; vertices are numbered 1 through n; |edge weight| ≤ 1,000,000,000. The total may need 64 bits.',
        'First line: n and m. Then m lines containing endpoint a, endpoint b, and weight.',
        'Print the minimum total weight, or IMPOSSIBLE if the graph is disconnected. One vertex needs no edges and has total weight 0.', tests)

    bags = [([(1, 1), (3, 4), (4, 5), (5, 7)], 7), ([(2, 10)], 1), ([], 0),
            ([(1, 0), (2, 0)], 3), ([(1, 1000000000)] * 4, 4), ([(3, 4)], 6)]
    for _ in range(12):
        items = [(RNG.randint(1, 20), RNG.randint(0, 50)) for _ in range(RNG.randint(0, 11))]
        bags.append((items, RNG.randint(0, 40)))
    tests = [(f'{len(items)} {capacity}\n' + ''.join(f'{weight} {value}\n' for weight, value in items),
              knapsack(items, capacity)) for items, capacity in bags]
    tests += [('100 20000\n' + '200 1000000000\n' * 100, 100000000000)]
    add('zero-one-knapsack', '0/1 Knapsack', 'HARD', ['Dynamic programming', 'Optimization'],
        'Choose items to place in a bag with a fixed weight capacity. Each item has a weight and a value, and each item may be taken at most once. Find the largest total value without exceeding the capacity. You do not need to fill the bag exactly.',
        '0 ≤ n ≤ 100; 0 ≤ capacity ≤ 20,000; 1 ≤ item weight ≤ 20,000; 0 ≤ item value ≤ 1,000,000,000. The answer may need 64 bits.',
        'First line: n and capacity. Then n lines containing weight and value.',
        'Print the maximum obtainable value.', tests)

    return result
