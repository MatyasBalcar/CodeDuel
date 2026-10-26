"""More standard algorithm exercises with repeatable examples, edge cases, and oracles."""
from collections import Counter
from functools import lru_cache
from itertools import combinations, permutations, product
from math import comb, factorial, isqrt
import posixpath
import random
import re

from common_catalog import array_input, longest_common_subsequence


RNG = random.Random(20261027)
MOD = 1000000007


def matrix_input(matrix):
    return f'{len(matrix)} {len(matrix[0])}\n' + ''.join(' '.join(map(str, row)) + '\n' for row in matrix)


def square_input(matrix):
    return f'{len(matrix)}\n' + ''.join(' '.join(map(str, row)) + '\n' for row in matrix)


def lines(matrix):
    return '\n'.join(' '.join(map(str, row)) for row in matrix)


def coin_combinations(coins, target):
    @lru_cache(None)
    def count(index, remaining):
        if index == len(coins):
            return int(remaining == 0)
        return sum(count(index + 1, remaining - amount)
                   for amount in range(0, remaining + 1, coins[index])) % MOD

    return count(0, target)


def grid_paths(grid):
    @lru_cache(None)
    def count(row, column):
        if row >= len(grid) or column >= len(grid[0]) or grid[row][column] == '#':
            return 0
        if row == len(grid) - 1 and column == len(grid[0]) - 1:
            return 1
        return (count(row + 1, column) + count(row, column + 1)) % MOD

    return count(0, 0)


def path_sum(matrix):
    @lru_cache(None)
    def minimum(row, column):
        if row == len(matrix) - 1 and column == len(matrix[0]) - 1:
            return matrix[row][column]
        choices = []
        if row + 1 < len(matrix):
            choices.append(minimum(row + 1, column))
        if column + 1 < len(matrix[0]):
            choices.append(minimum(row, column + 1))
        return matrix[row][column] + min(choices)

    return minimum(0, 0)


def triangle_sum(triangle):
    @lru_cache(None)
    def minimum(row, column):
        if row == len(triangle) - 1:
            return triangle[row][column]
        return triangle[row][column] + min(minimum(row + 1, column), minimum(row + 1, column + 1))

    return minimum(0, 0)


def falling_sum(matrix):
    @lru_cache(None)
    def minimum(row, column):
        if row == len(matrix) - 1:
            return matrix[row][column]
        return matrix[row][column] + min(minimum(row + 1, next_column)
                   for next_column in range(max(0, column - 1), min(len(matrix), column + 2)))

    return min(minimum(0, column) for column in range(len(matrix)))


def decode_ways(text):
    @lru_cache(None)
    def count(start):
        if start == len(text):
            return 1
        if text[start] == '0':
            return 0
        ways = count(start + 1)
        if start + 1 < len(text) and 10 <= int(text[start:start + 2]) <= 26:
            ways += count(start + 2)
        return ways % MOD

    return count(0)


def divide(first, second):
    quotient = abs(first) // abs(second)
    return -quotient if (first < 0) != (second < 0) else quotient


def expression(depth):
    if depth == 0 or RNG.randrange(3) == 0:
        value = RNG.randint(-10, 10)
        return [str(value)], value
    left, first = expression(depth - 1)
    right, second = expression(depth - 1)
    operator = RNG.choice('+-*' + ('/' if second != 0 else ''))
    if operator == '+':
        value = first + second
    elif operator == '-':
        value = first - second
    elif operator == '*':
        value = first * second
    else:
        value = divide(first, second)
    return left + right + [operator], value


def spiral(matrix):
    visited = set()
    row = column = direction = 0
    steps = [(0, 1), (1, 0), (0, -1), (-1, 0)]
    values = []
    for _ in range(len(matrix) * len(matrix[0])):
        values.append(matrix[row][column])
        visited.add((row, column))
        dr, dc = steps[direction]
        nr, nc = row + dr, column + dc
        if not (0 <= nr < len(matrix) and 0 <= nc < len(matrix[0])) or (nr, nc) in visited:
            direction = (direction + 1) % 4
            dr, dc = steps[direction]
        row, column = row + dr, column + dc
    return ' '.join(map(str, values))


def sudoku_valid(grid):
    units = [list(row) for row in grid]
    units += [[grid[row][column] for row in range(9)] for column in range(9)]
    units += [[grid[row][column] for row in range(top, top + 3) for column in range(left, left + 3)]
              for top in range(0, 9, 3) for left in range(0, 9, 3)]
    return all(len([value for value in unit if value != '.']) == len(set(unit) - {'.'}) for unit in units)


def word_search(grid, word):
    def visit(row, column, index, visited):
        if not (0 <= row < len(grid) and 0 <= column < len(grid[0])):
            return False
        if (row, column) in visited or grid[row][column] != word[index]:
            return False
        if index == len(word) - 1:
            return True
        used = visited | {(row, column)}
        return any(visit(nr, nc, index + 1, used) for nr, nc in
                   [(row - 1, column), (row + 1, column), (row, column - 1), (row, column + 1)])

    return any(visit(row, column, 0, set()) for row in range(len(grid)) for column in range(len(grid[0])))


def interval_removals(intervals):
    for kept in range(len(intervals), -1, -1):
        for chosen in combinations(intervals, kept):
            ordered = sorted(chosen)
            if all(ordered[index - 1][1] <= ordered[index][0] for index in range(1, kept)):
                return len(intervals) - kept


def schedule_time(tasks, cooldown):
    if not tasks:
        return 0
    best = float('inf')
    for order in set(permutations(tasks)):
        next_free = {}
        time = 0
        for task in order:
            time = max(time, next_free.get(task, 0))
            next_free[task] = time + cooldown + 1
            time += 1
        best = min(best, time)
    return best


def minimum_window(text, target):
    if not target:
        return ''
    required = Counter(target)
    best = None
    for start in range(len(text)):
        for end in range(start + 1, len(text) + 1):
            counts = Counter(text[start:end])
            if all(counts[char] >= count for char, count in required.items()):
                candidate = text[start:end]
                if best is None or len(candidate) < len(best):
                    best = candidate
                break
    return 'NONE' if best is None else best


def burst_balloons(values):
    @lru_cache(None)
    def burst(remaining):
        best = 0
        for index, value in enumerate(remaining):
            left = remaining[index - 1] if index else 1
            right = remaining[index + 1] if index + 1 < len(remaining) else 1
            best = max(best, left * value * right + burst(remaining[:index] + remaining[index + 1:]))
        return best

    return burst(tuple(values))


def palindrome_cuts(text):
    @lru_cache(None)
    def pieces(start):
        if start == len(text):
            return 0
        return 1 + min(pieces(end) for end in range(start + 1, len(text) + 1)
                       if text[start:end] == text[start:end][::-1])

    return max(0, pieces(0) - 1)


def increasing_path(matrix):
    @lru_cache(None)
    def longest(row, column):
        choices = [longest(nr, nc) for nr, nc in
                   [(row - 1, column), (row + 1, column), (row, column - 1), (row, column + 1)]
                   if 0 <= nr < len(matrix) and 0 <= nc < len(matrix[0]) and matrix[nr][nc] > matrix[row][column]]
        return 1 + max(choices, default=0)

    return max(longest(row, column) for row in range(len(matrix)) for column in range(len(matrix[0])))


def two_trades(prices):
    @lru_cache(None)
    def profit(day, holding, remaining):
        if day == len(prices):
            return -10**20 if holding else 0
        best = profit(day + 1, holding, remaining)
        if holding:
            best = max(best, prices[day] + profit(day + 1, False, remaining - 1))
        elif remaining:
            best = max(best, -prices[day] + profit(day + 1, True, remaining))
        return best

    return profit(0, False, 2)


def build_classics(problem, case):
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

    counts = [15, 5, 1, 3, 30, 100] + [RNG.randint(1, 200) for _ in range(10)] + [10000]
    add('fizz-buzz', 'Fizz Buzz', 'EASY', ['Math', 'Simulation'],
        'List the numbers from 1 through n. Replace multiples of 3 with Fizz, multiples of 5 with Buzz, and multiples of both with FizzBuzz.',
        '1 ≤ n ≤ 10,000.', 'One integer n.',
        'Print the n results in order on one line, separated by spaces.',
        [(f'{count}\n', ' '.join('FizzBuzz' if value % 15 == 0 else 'Fizz' if value % 3 == 0
          else 'Buzz' if value % 5 == 0 else str(value) for value in range(1, count + 1))) for count in counts])

    numbers = [5, 10, 0, 1, 25, 100] + [RNG.randint(0, 300) for _ in range(10)]
    tests = [(f'{value}\n', len(str(factorial(value))) - len(str(factorial(value)).rstrip('0'))) for value in numbers]
    value = 10**18
    tests += [(f'{value}\n', sum(value // 5**power for power in range(1, 27)))]
    add('factorial-trailing-zeroes', 'Factorial Trailing Zeroes', 'EASY', ['Math', 'Number theory'],
        'Count the zeros at the end of the decimal representation of n factorial. Do not construct the factorial itself. By definition, 0 factorial is 1.',
        '0 ≤ n ≤ 1,000,000,000,000,000,000.', 'One integer n.',
        'Print the number of trailing zeros.', tests)

    numbers = [11, 128, 0, 1, 2**63 - 1, 2**62] + [RNG.randrange(2**63) for _ in range(10)]
    add('count-set-bits', 'Count Set Bits', 'EASY', ['Bit manipulation'],
        'Count how many digits are 1 in the binary representation of a nonnegative integer.',
        '0 ≤ n ≤ 9,223,372,036,854,775,807.', 'One integer n.',
        'Print the number of set bits.', [(f'{value}\n', bin(value).count('1')) for value in numbers])

    numbers = [16, 3, 0, 1, -2, 2**59, 10**18] + [RNG.randint(-1000, 1000) for _ in range(10)]
    add('power-of-two', 'Power of Two', 'EASY', ['Math', 'Bit manipulation'],
        'Decide whether n is a power of two, meaning n equals 2 raised to a nonnegative integer power. Zero and negative numbers are not powers of two.',
        '−1,000,000,000,000,000,000 ≤ n ≤ 1,000,000,000,000,000,000.', 'One integer n.',
        'Print YES for a power of two, otherwise NO.',
        [(f'{value}\n', 'YES' if value > 0 and bin(value).count('1') == 1 else 'NO') for value in numbers])

    numbers = [123, -120, 0, 1534236469, -2147483648, 2147483647, -123] + [RNG.randint(-2**31, 2**31 - 1) for _ in range(10)]
    tests = []
    for value in numbers:
        reversed_value = int(str(abs(value))[::-1]) * (-1 if value < 0 else 1)
        tests.append((f'{value}\n', reversed_value if -2**31 <= reversed_value < 2**31 else 0))
    add('reverse-integer', 'Reverse Integer', 'EASY', ['Math'],
        'Reverse the decimal digits of a signed 32-bit integer, keeping its sign. Leading zeros in the reversed digits disappear. If the result is outside the signed 32-bit range, return zero.',
        '−2,147,483,648 ≤ n ≤ 2,147,483,647.', 'One integer n.',
        'Print the reversed integer, or 0 on overflow.', tests)

    numbers = [8, 16, 0, 1, 2, 10**18, 10**18 - 1] + [RNG.randrange(10**18 + 1) for _ in range(10)]
    add('integer-square-root', 'Integer Square Root', 'EASY', ['Math', 'Binary search'],
        'Find the largest nonnegative integer whose square is at most n. Return the floor of the square root without relying on floating-point rounding.',
        '0 ≤ n ≤ 1,000,000,000,000,000,000.', 'One integer n.',
        'Print the integer square root.', [(f'{value}\n', isqrt(value)) for value in numbers])

    columns = ['AB', 'ZY', 'A', 'Z', 'AAA', 'Z' * 12]
    columns += [''.join(RNG.choices('ABCDEFGHIJKLMNOPQRSTUVWXYZ', k=RNG.randint(1, 12))) for _ in range(10)]
    add('excel-column-number', 'Excel Column Number', 'EASY', ['Strings', 'Math'],
        'Spreadsheet columns are named A through Z, then AA, AB, and so on. Convert a column name to its 1-based column number. A represents 1 and Z represents 26.',
        'The name has 1 to 12 uppercase English letters. Use a 64-bit integer.',
        'One line containing the column name.', 'Print its column number.',
        [(text + '\n', sum((ord(char) - ord('A') + 1) * 26**power for power, char in enumerate(reversed(text)))) for text in columns])

    numerals = []
    for value in [3, 1994, 1, 4, 9, 58, 3999] + [RNG.randint(1, 3999) for _ in range(10)]:
        remaining = value
        text = ''
        for amount, symbol in [(1000, 'M'), (900, 'CM'), (500, 'D'), (400, 'CD'), (100, 'C'),
                               (90, 'XC'), (50, 'L'), (40, 'XL'), (10, 'X'), (9, 'IX'), (5, 'V'), (4, 'IV'), (1, 'I')]:
            text += symbol * (remaining // amount)
            remaining %= amount
        numerals.append((text + '\n', value))
    add('roman-to-integer', 'Roman to Integer', 'EASY', ['Strings', 'Math'],
        'Convert a valid Roman numeral to an integer. I, V, X, L, C, D, and M represent 1, 5, 10, 50, 100, 500, and 1000. In IV, IX, XL, XC, CD, and CM, the smaller symbol is subtracted from the larger one.',
        'The input is a standard valid Roman numeral representing an integer from 1 through 3999.',
        'One line containing the Roman numeral.', 'Print its integer value.', numerals)

    groups = [['flower', 'flow', 'flight'], ['dog', 'racecar', 'car'], [], [''], ['abc'], ['ab', 'abc', 'ab']]
    groups += [[''.join(RNG.choices('abc', k=RNG.randint(0, 12))) for _ in range(RNG.randint(0, 8))] for _ in range(10)]
    tests = []
    for words in groups:
        prefix = ''
        if words:
            for index in range(min(map(len, words))):
                if len({word[index] for word in words}) != 1:
                    break
                prefix += words[0][index]
        tests.append((f'{len(words)}\n' + '\n'.join(words) + '\n', prefix))
    tests += [('20\n' + ('a' * 10000 + '\n') * 20, 'a' * 10000)]
    add('longest-common-prefix', 'Longest Common Prefix', 'EASY', ['Strings'],
        'Find the longest prefix shared by every string in a list. A prefix starts at the first character. If the list is empty or no character is shared at the start, the answer is empty.',
        '0 ≤ n ≤ 200; each string has 0 to 10,000 lowercase English letters; their combined length is at most 200,000.',
        'First line: n. Then n lines, one string per line. String lines may be empty.',
        'Print the common prefix, or an empty line when none exists.', tests)

    texts = ['leetcode', 'aabb', '', 'z', 'loveleetcode', 'aabbc']
    texts += [''.join(RNG.choices('abcde', k=RNG.randint(0, 50))) for _ in range(10)]
    tests = [(text + '\n', next((index for index, char in enumerate(text) if text.count(char) == 1), -1)) for text in texts]
    tests += [('a' * 199999 + 'b\n', 199999)]
    add('first-unique-character', 'First Unique Character', 'EASY', ['Strings', 'Counting'],
        'Find the first character that appears exactly once in a string. Return its zero-based position.',
        '0 ≤ length ≤ 200,000. Characters are lowercase English letters.', 'One line containing the string, possibly empty.',
        'Print the first unique character position, or -1 if there is none.', tests)

    pairs = [('abc', 'ahbgdc'), ('axc', 'ahbgdc'), ('', ''), ('', 'abc'), ('a', ''), ('aaa', 'aa')]
    pairs += [(''.join(RNG.choices('abc', k=RNG.randint(0, 15))), ''.join(RNG.choices('abc', k=RNG.randint(0, 30)))) for _ in range(10)]
    tests = []
    for first, second in pairs:
        position = 0
        possible = True
        for char in first:
            found = second.find(char, position)
            if found < 0:
                possible = False
                break
            position = found + 1
        tests.append((first + '\n' + second + '\n', 'YES' if possible else 'NO'))
    tests += [('a' * 100000 + '\n' + 'ab' * 100000 + '\n', 'YES')]
    add('is-subsequence', 'Is Subsequence', 'EASY', ['Strings', 'Two pointers'],
        'Decide whether the first string can be obtained by deleting zero or more characters from the second without changing the order of the remaining characters.',
        'Each string has 0 to 200,000 lowercase English letters.', 'Two lines, one string per line. Either may be empty.',
        'Print YES if the first string is a subsequence of the second, otherwise NO.', tests)

    pairs = [('egg', 'add'), ('foo', 'bar'), ('', ''), ('ab', 'aa'), ('paper', 'title'), ('a', '')]
    pairs += [(''.join(RNG.choices('abc', k=RNG.randint(0, 20))), ''.join(RNG.choices('xyz', k=RNG.randint(0, 20)))) for _ in range(10)]
    tests = [(first + '\n' + second + '\n', 'YES' if len(first) == len(second)
              and len(set(zip(first, second))) == len(set(first)) == len(set(second)) else 'NO') for first, second in pairs]
    tests += [('ab' * 100000 + '\n' + 'xy' * 100000 + '\n', 'YES')]
    add('isomorphic-strings', 'Isomorphic Strings', 'EASY', ['Strings', 'Hash maps'],
        'Decide whether every character in the first string can be consistently replaced to make the second string. Different source characters must map to different destination characters. Character order cannot change.',
        'Each string has 0 to 200,000 lowercase English letters.', 'Two lines, one string per line. Either may be empty.',
        'Print YES if a one-to-one character mapping exists, otherwise NO.', tests)

    matrices = [[[1, 2, 3], [4, 5, 6], [7, 8, 9]], [[5]], [[1, 1], [1, 1]]]
    matrices += [[[RNG.randint(-100, 100) for _ in range(size)] for _ in range(size)] for size in [RNG.randint(1, 8) for _ in range(10)]]
    tests = [(square_input(matrix), sum(value for row, values in enumerate(matrix) for column, value in enumerate(values)
              if row == column or row + column == len(matrix) - 1)) for matrix in matrices]
    tests += [(square_input([[1000000000] * 300 for _ in range(300)]), 600000000000)]
    add('matrix-diagonal-sum', 'Matrix Diagonal Sum', 'EASY', ['Arrays', 'Matrices'],
        'Add all values on the main diagonal and the opposite diagonal of a square matrix. If a cell belongs to both diagonals, count it only once.',
        '1 ≤ n ≤ 300; |value| ≤ 1,000,000,000. The sum may need 64 bits.',
        'First line: n. Then n rows of n integers.', 'Print the sum of the two diagonals without double-counting their intersection.', tests)

    matrices = [[[1, 2, 3], [4, 5, 6]], [[1]], [[1], [2], [3]]]
    for _ in range(10):
        rows, columns = RNG.randint(1, 7), RNG.randint(1, 7)
        matrices.append([[RNG.randint(-100, 100) for _ in range(columns)] for _ in range(rows)])
    tests = [(matrix_input(matrix), lines(list(zip(*matrix)))) for matrix in matrices]
    tests += [(matrix_input([[0] * 100 for _ in range(100)]), lines([[0] * 100 for _ in range(100)]))]
    add('transpose-matrix', 'Transpose Matrix', 'EASY', ['Arrays', 'Matrices'],
        'Transpose a matrix by turning each original row into a column. An input with r rows and c columns produces c rows and r columns.',
        '1 ≤ rows, columns ≤ 100; |value| ≤ 1,000,000,000.',
        'First line: rows and columns. Then rows lines of column values.', 'Print the transposed matrix, one row per line.', tests)

    arrays = [[-4, -1, 0, 3, 10], [-7, -3, 2, 3, 11], [], [0], [-1000000000, 1000000000]]
    arrays += [sorted(RNG.randint(-100, 100) for _ in range(RNG.randint(0, 40))) for _ in range(10)]
    tests = [(array_input(values), ' '.join(map(str, sorted(value * value for value in values)))) for values in arrays]
    tests += [(array_input([-1] * 10000), ' '.join(['1'] * 10000))]
    add('sorted-squares', 'Squares of a Sorted Array', 'EASY', ['Arrays', 'Two pointers'],
        'Square every value in a nondecreasing array and return the squares in nondecreasing order. Negative values may appear. Aim for linear time.',
        '0 ≤ n ≤ 100,000; |value| ≤ 1,000,000,000. Squared values may need 64 bits.',
        'First line: n. Second line: n sorted integers.', 'Print the sorted squares, or an empty line when n is zero.', tests)

    digits = [[1, 2, 3], [9, 9], [0], [4, 9, 9], [1]]
    digits += [[RNG.randint(1, 9)] + [RNG.randint(0, 9) for _ in range(RNG.randint(0, 25))] for _ in range(10)]
    tests = [(array_input(values), ' '.join(str(int(''.join(map(str, values))) + 1))) for values in digits]
    tests += [(array_input([9] * 10000), '1 ' + ' '.join(['0'] * 10000))]
    add('plus-one', 'Plus One', 'EASY', ['Arrays', 'Math'],
        'An array stores a nonnegative decimal integer, one digit per position, most significant digit first. Add one and return the resulting digits. The input has no leading zero unless the number is zero.',
        '1 ≤ n ≤ 10,000; each digit is from 0 through 9.',
        'First line: n. Second line: n digits separated by spaces.', 'Print the digits after adding one, separated by spaces.', tests)

    pairs = [('11', '1'), ('1010', '1011'), ('0', '0'), ('1', '0'), ('111', '111')]
    for _ in range(10):
        pairs.append((bin(RNG.randrange(2**60))[2:], bin(RNG.randrange(2**60))[2:]))
    pairs += [('1' * 10000, '1')]
    add('add-binary', 'Add Binary', 'EASY', ['Strings', 'Math'],
        'Add two nonnegative integers written in binary. Return the sum in binary without leading zeros, except that zero is written as 0.',
        'Each input has 1 to 10,000 binary digits and no unnecessary leading zeros.', 'Two lines, one binary integer per line.',
        'Print their binary sum.', [(first + '\n' + second + '\n', bin(int(first, 2) + int(second, 2))[2:]) for first, second in pairs])

    pairs = [('aa', 'aab'), ('aa', 'ab'), ('', ''), ('a', ''), ('abc', 'cbad')]
    pairs += [(''.join(RNG.choices('abcde', k=RNG.randint(0, 30))), ''.join(RNG.choices('abcde', k=RNG.randint(0, 50)))) for _ in range(10)]
    tests = [(note + '\n' + magazine + '\n', 'YES' if not (Counter(note) - Counter(magazine)) else 'NO') for note, magazine in pairs]
    tests += [('a' * 200000 + '\n' + 'a' * 200000 + '\n', 'YES')]
    add('ransom-note', 'Ransom Note', 'EASY', ['Strings', 'Counting'],
        'Decide whether the first string can be built using letters from the second string. Each occurrence in the second string can be used at most once, and letters may be rearranged.',
        'Each string has 0 to 200,000 lowercase English letters.', 'First line: the note. Second line: the available letters. Either may be empty.',
        'Print YES if the note can be formed, otherwise NO.', tests)

    texts = ['Hello World', '   fly me   to   the moon  ', '', '   ', 'a', 'hello']
    texts += [''.join(RNG.choices('abc ', k=RNG.randint(0, 80))) for _ in range(10)]
    tests = [(text + '\n', len(text.split()[-1]) if text.split() else 0) for text in texts]
    tests += [('word ' + 'a' * 199995 + '\n', 199995)]
    add('length-of-last-word', 'Length of Last Word', 'EASY', ['Strings'],
        'Find the number of letters in the last word of a line. Words are separated by spaces, and extra spaces may appear at either end. A line containing no word has answer zero.',
        '0 ≤ line length ≤ 200,000; characters are English letters or spaces.', 'One line of text, possibly empty.',
        'Print the length of its last word.', tests)

    rows = [3, 0, 1, 2, 10, 60] + [RNG.randint(0, 60) for _ in range(10)]
    add('pascals-triangle-row', "Pascal's Triangle Row", 'EASY', ['Arrays', 'Math'],
        'Return row k of Pascal\'s triangle, counting the top row [1] as row 0. Each interior value is the sum of the two values above it, and each row begins and ends with 1.',
        '0 ≤ k ≤ 60. Values may need 64-bit integers.', 'One integer k.',
        'Print the row values separated by spaces.', [(f'{row}\n', ' '.join(str(comb(row, column)) for column in range(row + 1))) for row in rows])

    arrays = [[-1, 0, 1, 2, -1, -4], [0, 0, 0], [], [1, 2], [-2, 0, 0, 2, 2]]
    arrays += [[RNG.randint(-8, 8) for _ in range(RNG.randint(0, 18))] for _ in range(10)]
    tests = [(array_input(values), len({tuple(sorted(items)) for items in combinations(values, 3) if sum(items) == 0})) for values in arrays]
    tests += [(array_input([0] * 3000), 1)]
    add('three-sum', 'Three Sum', 'MEDIUM', ['Arrays', 'Sorting', 'Two pointers'],
        'Count the different value triplets that add to zero. Choose three different array positions, but count a triplet only once regardless of position choices or value order. For example, many zeroes still produce just the triplet (0, 0, 0).',
        '0 ≤ n ≤ 3,000; |value| ≤ 1,000,000,000.', 'First line: n. Second line: n integers.',
        'Print the number of distinct zero-sum value triplets.', tests)

    quadruples = [([1, 2], [-2, -1], [-1, 2], [0, 2]), ([0], [0], [0], [0]), ([], [], [], [])]
    quadruples += [tuple([RNG.randint(-5, 5) for _ in range(count)] for _ in range(4)) for count in [RNG.randint(0, 6) for _ in range(10)]]
    tests = [(f'{len(groups[0])}\n' + ''.join(' '.join(map(str, values)) + '\n' for values in groups),
              sum(sum(items) == 0 for items in product(*groups))) for groups in quadruples]
    tests += [('500\n' + ('0 ' * 500 + '\n') * 4, 62500000000)]
    add('four-sum-count', 'Four Sum Count', 'MEDIUM', ['Arrays', 'Hash maps'],
        'Four arrays A, B, C, and D have the same length. Count the index quadruples (i, j, k, l) for which A[i] + B[j] + C[k] + D[l] equals zero. Equal values at different positions count as different choices.',
        '0 ≤ n ≤ 500; |value| ≤ 1,000,000,000. The answer may need 64 bits.',
        'First line: n. Then four lines, one array per line, each containing n integers.', 'Print the number of zero-sum index quadruples.', tests)

    arrays = [[100, 4, 200, 1, 3, 2], [0, 3, 7, 2, 5, 8, 4, 6, 0, 1], [], [1], [2, 2, 2]]
    arrays += [[RNG.randint(-20, 20) for _ in range(RNG.randint(0, 50))] for _ in range(10)]
    tests = []
    for values in arrays:
        ordered = sorted(set(values))
        best = current = 0
        previous = None
        for value in ordered:
            current = current + 1 if previous is not None and value == previous + 1 else 1
            best = max(best, current)
            previous = value
        tests.append((array_input(values), best))
    tests += [(array_input(list(range(99999, -1, -1))), 100000)]
    add('longest-consecutive-sequence', 'Longest Consecutive Sequence', 'MEDIUM', ['Arrays', 'Hash sets'],
        'Find the longest run of consecutive integer values present in an unsorted array. Their positions do not matter. Repeated values cannot extend a run. Aim for linear expected time.',
        '0 ≤ n ≤ 100,000; |value| ≤ 1,000,000,000.', 'First line: n. Second line: n integers.',
        'Print the longest consecutive run length.', tests)

    arrays = [[1, 2, 3, 4], [-1, 1, 0, -3, 3], [7], [0, 0], [0, 1000000000, 1000000000]]
    arrays += [[RNG.randint(-3, 3) for _ in range(RNG.randint(1, 14))] for _ in range(10)]
    tests = []
    for values in arrays:
        answers = []
        for excluded in range(len(values)):
            answer = 1
            for index, value in enumerate(values):
                if index != excluded:
                    answer *= value
            answers.append(answer)
        tests.append((array_input(values), ' '.join(map(str, answers))))
    tests += [(array_input([1] * 10000), ' '.join(['1'] * 10000))]
    add('product-except-self', 'Product of Array Except Self', 'MEDIUM', ['Arrays', 'Prefix products'],
        'For each position, multiply every array value except the value at that position. Do not use division. The product of no values is 1, so a one-element array produces [1]. Aim for linear time.',
        '1 ≤ n ≤ 100,000; |value| ≤ 1,000,000,000. The absolute product of all nonzero values is at most 10^18; every answer fits a signed 64-bit integer.',
        'First line: n. Second line: n integers.', 'Print the product for each position, in original order.', tests)

    windows = [([2, 3, 1, 2, 4, 3], 7), ([1, 1, 1], 5), ([], 1), ([5], 5), ([1, 2, 3], 1)]
    windows += [([RNG.randint(1, 10) for _ in range(RNG.randint(0, 35))], RNG.randint(1, 100)) for _ in range(10)]
    tests = [(f'{len(values)} {target}\n' + ' '.join(map(str, values)) + '\n',
              min((end - start for start in range(len(values)) for end in range(start + 1, len(values) + 1)
                   if sum(values[start:end]) >= target), default=0)) for values, target in windows]
    tests += [('100000 100000\n' + '1 ' * 100000 + '\n', 100000)]
    add('minimum-size-subarray-sum', 'Minimum Size Subarray Sum', 'MEDIUM', ['Arrays', 'Sliding window'],
        'Find the shortest nonempty contiguous part of a positive array whose sum is at least the target. Positive values allow a sliding-window solution.',
        '0 ≤ n ≤ 100,000; 1 ≤ value ≤ 1,000,000,000; 1 ≤ target ≤ 10^18.',
        'First line: n and target. Second line: n positive integers.', 'Print the minimum length, or 0 if no part qualifies.', tests)

    replacements = [('ABAB', 2), ('AABABBA', 1), ('', 0), ('A', 0), ('AAAA', 0), ('ABCD', 4)]
    replacements += [(''.join(RNG.choices('ABCD', k=count)), RNG.randint(0, count)) for count in [RNG.randint(0, 30) for _ in range(10)]]
    tests = []
    for text, limit in replacements:
        answer = max((end - start for start in range(len(text)) for end in range(start + 1, len(text) + 1)
                      if end - start - max(Counter(text[start:end]).values()) <= limit), default=0)
        tests.append((f'{limit}\n{text}\n', answer))
    tests += [('25000\n' + 'AB' * 50000 + '\n', 50001)]
    add('character-replacement', 'Longest Repeating Character Replacement', 'MEDIUM', ['Strings', 'Sliding window'],
        'You may replace at most k characters in a string. Find the greatest length of a contiguous part that can be made entirely of one repeated character.',
        '0 ≤ length ≤ 100,000; 0 ≤ k ≤ length. Characters are uppercase English letters.',
        'First line: k. Second line: the string, possibly empty.', 'Print the greatest achievable repeated-character length.', tests)

    products = [([10, 5, 2, 6], 100), ([1, 2, 3], 0), ([], 5), ([1, 1, 1], 2), ([1000000, 1000000], 10**12)]
    products += [([RNG.randint(1, 9) for _ in range(RNG.randint(0, 25))], RNG.randint(0, 500)) for _ in range(10)]
    tests = []
    for values, limit in products:
        answer = 0
        for start in range(len(values)):
            total = 1
            for value in values[start:]:
                total *= value
                answer += total < limit
        tests.append((f'{len(values)} {limit}\n' + ' '.join(map(str, values)) + '\n', answer))
    tests += [('100000 2\n' + '1 ' * 100000 + '\n', 5000050000)]
    add('subarray-product-less-than-k', 'Subarray Product Less Than K', 'MEDIUM', ['Arrays', 'Sliding window'],
        'Count nonempty contiguous parts of an array whose product is strictly smaller than k. All array values are positive.',
        '0 ≤ n ≤ 100,000; 1 ≤ value ≤ 1,000,000; 0 ≤ k ≤ 10^12. The count may need 64 bits.',
        'First line: n and k. Second line: n positive integers.', 'Print the number of qualifying subarrays.', tests)

    arrays = [[1, 5, 11, 5], [1, 2, 3, 5], [], [1], [2, 2], [100, 1, 1]]
    arrays += [[RNG.randint(1, 20) for _ in range(RNG.randint(0, 12))] for _ in range(10)]
    tests = []
    for values in arrays:
        total = sum(values)
        possible = total % 2 == 0 and any(sum(chosen) * 2 == total for size in range(len(values) + 1)
                                         for chosen in combinations(values, size))
        tests.append((array_input(values), 'YES' if possible else 'NO'))
    tests += [(array_input([100] * 200), 'YES')]
    add('partition-equal-subset-sum', 'Partition Equal Subset Sum', 'MEDIUM', ['Arrays', 'Dynamic programming'],
        'Decide whether all array positions can be divided into two groups with equal sums. Each position must belong to exactly one group. The groups may be empty, so an empty array succeeds.',
        '0 ≤ n ≤ 200; 1 ≤ value ≤ 100.', 'First line: n. Second line: n positive integers.',
        'Print YES if an equal partition exists, otherwise NO.', tests)

    coins = [([1, 2, 5], 5), ([2], 3), ([], 0), ([], 5), ([5], 0)]
    coins += [(RNG.sample(range(1, 16), RNG.randint(0, 6)), RNG.randint(0, 40)) for _ in range(10)]
    coins += [(list(range(1, 21)), 200)]
    tests = [(f'{len(values)} {target}\n' + ' '.join(map(str, values)) + '\n', coin_combinations(values, target))
             for values, target in coins]
    tests += [('2 10000\n1 2\n', 5001)]
    add('coin-change-combinations', 'Coin Change Combinations', 'MEDIUM', ['Dynamic programming', 'Counting'],
        'Count the ways to make the target using unlimited coins of the given denominations. Coin order does not matter. For example, 1 + 2 and 2 + 1 are the same combination. The empty choice makes target zero in one way.',
        '0 ≤ coin count ≤ 100; 0 ≤ target ≤ 10,000; denominations are distinct integers from 1 through 10,000.',
        'First line: coin count and target. Second line: the denominations.', 'Print the combination count modulo 1,000,000,007.', tests)

    grids = [['...', '.#.', '...'], ['.#', '..'], ['.'], ['#'], ['..#']]
    for _ in range(10):
        rows, columns = RNG.randint(1, 6), RNG.randint(1, 6)
        grids.append([''.join(RNG.choices('.#', weights=[4, 1], k=columns)) for _ in range(rows)])
    tests = [(f'{len(grid)} {len(grid[0])}\n' + '\n'.join(grid) + '\n', grid_paths(grid)) for grid in grids]
    tests += [('200 200\n' + ('.' * 200 + '\n') * 200, comb(398, 199) % MOD)]
    add('unique-paths-with-obstacles', 'Unique Paths With Obstacles', 'MEDIUM', ['Matrices', 'Dynamic programming'],
        'Count routes from the top-left cell to the bottom-right cell. Every move must go one cell right or one cell down, and walls cannot be entered. A blocked start or destination gives zero routes.',
        '1 ≤ rows, columns ≤ 200. A dot is open and # is a wall.', 'First line: rows and columns. Then the grid rows.',
        'Print the route count modulo 1,000,000,007.', tests)

    matrices = [[[1, 3, 1], [1, 5, 1], [4, 2, 1]], [[1, 2, 3], [4, 5, 6]], [[0]]]
    for _ in range(10):
        rows, columns = RNG.randint(1, 7), RNG.randint(1, 7)
        matrices.append([[RNG.randint(0, 20) for _ in range(columns)] for _ in range(rows)])
    tests = [(matrix_input(matrix), path_sum(matrix)) for matrix in matrices]
    tests += [(matrix_input([[1000000000] * 300 for _ in range(300)]), 599000000000)]
    add('minimum-path-sum', 'Minimum Path Sum', 'MEDIUM', ['Matrices', 'Dynamic programming'],
        'Travel from the top-left to the bottom-right of a matrix, moving only right or down. Pay the value of every visited cell, including both ends. Find the smallest total cost.',
        '1 ≤ rows, columns ≤ 300; 0 ≤ cell value ≤ 1,000,000,000. The total may need 64 bits.',
        'First line: rows and columns. Then the matrix rows.', 'Print the minimum path sum.', tests)

    triangles = [[[2], [3, 4], [6, 5, 7], [4, 1, 8, 3]], [[-10]], [[1], [2, 3]]]
    triangles += [[[RNG.randint(-20, 20) for _ in range(row + 1)] for row in range(size)]
                  for size in [RNG.randint(1, 7) for _ in range(10)]]
    tests = [(f'{len(triangle)}\n' + lines(triangle) + '\n', triangle_sum(triangle)) for triangle in triangles]
    tests += [('400\n' + lines([[-1000000] * (row + 1) for row in range(400)]) + '\n', -400000000)]
    add('triangle-minimum-path-sum', 'Triangle Minimum Path Sum', 'MEDIUM', ['Arrays', 'Dynamic programming'],
        'Start at the top of a number triangle. From position j in a row, move to position j or j + 1 in the next row. Find the smallest sum from top to bottom, counting every visited number.',
        '1 ≤ row count ≤ 400; |value| ≤ 1,000,000. Row i has i values, counting from 1.',
        'First line: the row count. Then the triangle rows.', 'Print the minimum path sum.', tests)

    matrices = [[[2, 1, 3], [6, 5, 4], [7, 8, 9]], [[-19, 57], [-40, -5]], [[7]]]
    matrices += [[[RNG.randint(-20, 20) for _ in range(size)] for _ in range(size)]
                 for size in [RNG.randint(1, 6) for _ in range(10)]]
    tests = [(square_input(matrix), falling_sum(matrix)) for matrix in matrices]
    tests += [(square_input([[1000000000] * 300 for _ in range(300)]), 300000000000)]
    add('minimum-falling-path-sum', 'Minimum Falling Path Sum', 'MEDIUM', ['Matrices', 'Dynamic programming'],
        'Start at any cell of the first row of a square matrix and reach the last row. Each move goes down one row and changes the column by -1, 0, or 1, staying inside the matrix. Find the minimum sum of visited cells.',
        '1 ≤ n ≤ 300; |cell value| ≤ 1,000,000,000. The sum may need 64 bits.',
        'First line: n. Then n rows of n integers.', 'Print the minimum falling path sum.', tests)

    texts = ['babad', 'cbbd', '', 'a', 'aaaa', 'abc']
    texts += [''.join(RNG.choices('abcd', k=RNG.randint(0, 35))) for _ in range(10)]
    tests = [(text + '\n', max((end - start for start in range(len(text)) for end in range(start + 1, len(text) + 1)
              if text[start:end] == text[start:end][::-1]), default=0)) for text in texts]
    tests += [('a' * 5000 + '\n', 5000)]
    add('longest-palindromic-substring', 'Longest Palindromic Substring', 'MEDIUM', ['Strings', 'Two pointers'],
        'Find the length of the longest contiguous part of a string that reads the same forwards and backwards. Only the length is required, so equally long answers do not need a tie-break.',
        '0 ≤ length ≤ 5,000. Characters are lowercase English letters.', 'One line containing the string, possibly empty.',
        'Print the longest palindromic substring length.', tests)

    texts = ['12', '226', '0', '06', '10', '2101', '11106', '1' * 200]
    texts += [''.join(RNG.choices('0123456789', k=RNG.randint(1, 25))) for _ in range(10)]
    tests = [(text + '\n', decode_ways(text)) for text in texts]
    tests += [('10' * 50000 + '\n', 1)]
    add('decode-ways', 'Decode Ways', 'MEDIUM', ['Strings', 'Dynamic programming'],
        'The numbers 1 through 26 encode the letters A through Z. Count the ways to split a digit string into valid encoded letters. A token cannot begin with zero, so 0 and 06 are invalid while 10 and 20 are valid.',
        '1 ≤ length ≤ 100,000. Every character is a decimal digit.', 'One line containing the encoded digits.',
        'Print the decoding count modulo 1,000,000,007.', tests)

    expressions = [(['2', '1', '+', '3', '*'], 9), (['4', '13', '5', '/', '+'], 6),
                   (['-7', '3', '/'], -2), (['5'], 5), (['1000000', '1000000', '*'], 1000000000000)]
    expressions += [expression(3) for _ in range(10)]
    tests = [(f'{len(tokens)}\n' + ' '.join(tokens) + '\n', value) for tokens, value in expressions]
    tests += [('99999\n1 ' + '1 + ' * 49999 + '\n', 50000)]
    add('evaluate-reverse-polish-notation', 'Evaluate Reverse Polish Notation', 'MEDIUM', ['Stacks', 'Math'],
        'Evaluate a valid postfix expression. A number pushes a value; an operator consumes the two previous values, with the earlier one on the left. Operators are +, -, *, and /. Integer division truncates toward zero.',
        '1 ≤ token count ≤ 100,000; operands are between -1,000,000 and 1,000,000. Every intermediate result fits a signed 64-bit integer. Division by zero never occurs, and the expression leaves exactly one result.',
        'First line: token count. Second line: the space-separated postfix tokens.', 'Print the final value.', tests)

    paths = ['/home/', '/a/./b/../../c/', '/', '/../../', '/a//b///c', '/.../a/../.hidden']
    for _ in range(10):
        paths.append('/' + '/'.join(RNG.choices(['a', 'b', '.', '..', '', '...'], k=RNG.randint(1, 25))))
    paths += ['/a/..' * 30000]
    add('simplify-unix-path', 'Simplify Unix Path', 'MEDIUM', ['Strings', 'Stacks'],
        'Simplify an absolute Unix-style path. Repeated slashes act as one slash, . stays in the current directory, and .. moves to the parent. Moving above root stays at root. Other names, including ..., are ordinary directory names.',
        '1 ≤ path length ≤ 200,000. The path starts with /. Components use English letters and dots.', 'One line containing the path.',
        'Print the canonical path with a single leading slash and no trailing slash unless it is root.',
        [(path + '\n', posixpath.normpath('/' + path.lstrip('/'))) for path in paths])

    frequent = [([1, 1, 1, 2, 2, 3], 2), ([4, 4, 2, 2], 2), ([], 0), ([5], 0)]
    for _ in range(10):
        values = [RNG.randint(-8, 8) for _ in range(RNG.randint(1, 50))]
        frequent.append((values, RNG.randint(0, len(set(values)))))
    tests = [(f'{len(values)} {count}\n' + ' '.join(map(str, values)) + '\n',
              ' '.join(map(str, sorted(Counter(values), key=lambda value: (-values.count(value), value))[:count])))
             for values, count in frequent]
    tests += [('100000 1\n' + '7 ' * 100000 + '\n', '7')]
    add('top-k-frequent-elements', 'Top K Frequent Elements', 'MEDIUM', ['Arrays', 'Hash maps', 'Sorting'],
        'Return the k most frequent distinct array values. Order them by decreasing occurrence count. If two values have the same count, put the smaller value first.',
        '0 ≤ n ≤ 100,000; 0 ≤ k ≤ the number of distinct values; |value| ≤ 1,000,000,000.',
        'First line: n and k. Second line: n integers.', 'Print the k values in the required order, or an empty line when k is zero.', tests)

    rankings = [([3, 2, 1, 5, 6, 4], 2), ([3, 2, 3, 1, 2, 4, 5, 5, 6], 4), ([5], 1), ([-1, -2, -3], 3)]
    rankings += [(values, RNG.randint(1, len(values))) for values in
                 [[RNG.randint(-50, 50) for _ in range(RNG.randint(1, 50))] for _ in range(10)]]
    tests = [(f'{len(values)} {rank}\n' + ' '.join(map(str, values)) + '\n', sorted(values, reverse=True)[rank - 1])
             for values, rank in rankings]
    tests += [('100000 50000\n' + ' '.join(map(str, range(100000))) + '\n', 50000)]
    add('kth-largest-element', 'Kth Largest Element', 'MEDIUM', ['Arrays', 'Heaps'],
        'Find the kth largest array value, counting duplicate positions separately. The largest value has rank 1.',
        '1 ≤ k ≤ n ≤ 100,000; |value| ≤ 1,000,000,000.', 'First line: n and k. Second line: n integers.',
        'Print the kth largest value.', tests)

    matrices = [([[1, 5, 9], [10, 11, 13], [12, 13, 15]], 8), ([[-5]], 1), ([[1, 1], [1, 2]], 3)]
    for _ in range(10):
        size = RNG.randint(1, 7)
        row_base = sorted(RNG.randint(-20, 20) for _ in range(size))
        col_base = sorted(RNG.randint(-20, 20) for _ in range(size))
        matrix = [[a + b for b in col_base] for a in row_base]
        matrices.append((matrix, RNG.randint(1, size * size)))
    tests = [(f'{len(matrix)} {rank}\n' + lines(matrix) + '\n', sorted(value for row in matrix for value in row)[rank - 1])
             for matrix, rank in matrices]
    tests += [('200 40000\n' + lines([[row + column for column in range(200)] for row in range(200)]) + '\n', 398)]
    add('kth-smallest-sorted-matrix', 'Kth Smallest in a Sorted Matrix', 'MEDIUM', ['Matrices', 'Binary search'],
        'Every row and every column of a square matrix is sorted in nondecreasing order. Find its kth smallest value, counting repeated cells separately.',
        '1 ≤ n ≤ 200; 1 ≤ k ≤ n squared; |value| ≤ 1,000,000,000.',
        'First line: n and k. Then n rows of n sorted values.', 'Print the kth smallest value.', tests)

    matrices = [[[1, 2, 3], [4, 5, 6], [7, 8, 9]], [[1, 2, 3, 4], [5, 6, 7, 8], [9, 10, 11, 12]], [[1]], [[1], [2], [3]]]
    for _ in range(10):
        rows, columns = RNG.randint(1, 7), RNG.randint(1, 7)
        matrices.append([[RNG.randint(-50, 50) for _ in range(columns)] for _ in range(rows)])
    tests = [(matrix_input(matrix), spiral(matrix)) for matrix in matrices]
    tests += [(matrix_input([[0] * 100 for _ in range(100)]), ' '.join(['0'] * 10000))]
    add('spiral-matrix', 'Spiral Matrix', 'MEDIUM', ['Matrices', 'Simulation'],
        'Read a matrix in clockwise spiral order. Start at the top-left, move across the top row, then down the right edge, left along the bottom, and up the left edge. Continue inward until every cell is visited once.',
        '1 ≤ rows, columns ≤ 100; |value| ≤ 1,000,000,000.', 'First line: rows and columns. Then the matrix rows.',
        'Print all values in spiral order on one line.', tests)

    solved = [''.join(str((row * 3 + row // 3 + column) % 9 + 1) for column in range(9)) for row in range(9)]
    boards = [solved, ['11.......'] + ['.........'] * 8, ['.........'] * 9]
    for _ in range(10):
        board = [''.join(value if RNG.randrange(3) == 0 else '.' for value in row) for row in solved]
        boards.append(board)
        invalid = board.copy()
        invalid[0] = '7' + invalid[0][1:8] + '7'
        boards.append(invalid)
    add('valid-sudoku', 'Valid Sudoku', 'MEDIUM', ['Matrices', 'Hash sets'],
        'Check a partially filled 9 by 9 Sudoku board. Filled digits must not repeat in a row, column, or 3 by 3 box. Empty cells are dots. You only need to check the filled cells, not solve the puzzle or prove that a solution exists.',
        'Exactly nine rows of nine characters, each a dot or a digit from 1 through 9.',
        'Nine board rows with no spaces.', 'Print YES if all filled cells obey the rules, otherwise NO.',
        [('\n'.join(board) + '\n', 'YES' if sudoku_valid(board) else 'NO') for board in boards])

    searches = [(['ABCE', 'SFCS', 'ADEE'], 'ABCCED'), (['ABCE', 'SFCS', 'ADEE'], 'ABCB'), (['A'], 'A'), (['A'], 'AA')]
    for _ in range(10):
        rows, columns = RNG.randint(1, 4), RNG.randint(1, 4)
        searches.append(([''.join(RNG.choices('ABC', k=columns)) for _ in range(rows)],
                         ''.join(RNG.choices('ABC', k=RNG.randint(1, 7)))))
    tests = [(f'{len(grid)} {len(grid[0])}\n' + '\n'.join(grid) + '\n' + word + '\n',
              'YES' if word_search(grid, word) else 'NO') for grid, word in searches]
    tests += [('12 12\n' + ('A' * 12 + '\n') * 12 + 'A' * 12 + '\n', 'YES')]
    add('word-search', 'Word Search', 'MEDIUM', ['Matrices', 'Backtracking'],
        'Find a word in a character grid. Consecutive letters must be horizontally or vertically adjacent. A grid cell may be used at most once in the same word. The word may start at any cell.',
        '1 ≤ rows, columns ≤ 12; 1 ≤ word length ≤ 12. Grid and word use uppercase English letters.',
        'First line: rows and columns. Then the grid rows. Last line: the word.', 'Print YES if the word can be traced, otherwise NO.', tests)

    intervals = [[(1, 2), (2, 3), (3, 4), (1, 3)], [(1, 2), (1, 2), (1, 2)], [], [(0, 1)], [(-5, -1), (-1, 3)]]
    intervals += [[(start, start + RNG.randint(1, 8)) for start in [RNG.randint(-10, 10) for _ in range(RNG.randint(0, 10))]] for _ in range(10)]
    tests = [(f'{len(items)}\n' + ''.join(f'{a} {b}\n' for a, b in items), interval_removals(items)) for items in intervals]
    tests += [('100000\n' + '0 1\n' * 100000, 99999)]
    add('non-overlapping-intervals', 'Non-overlapping Intervals', 'MEDIUM', ['Intervals', 'Greedy', 'Sorting'],
        'Remove as few intervals as possible so that none of the remaining intervals overlap. An interval includes its start and excludes its end, so [1, 2) and [2, 3) can both remain.',
        '0 ≤ n ≤ 100,000; −1,000,000,000 ≤ start < end ≤ 1,000,000,000.',
        'First line: n. Then n lines containing start and end.', 'Print the minimum number of removed intervals.', tests)

    tasks = [('AAABBB', 2), ('AAABBB', 0), ('', 3), ('A', 100), ('AA', 1)]
    tasks += [(''.join(RNG.choices('ABC', k=RNG.randint(0, 7))), RNG.randint(0, 3)) for _ in range(10)]
    tests = [(f'{len(text)} {cooldown}\n{text}\n', schedule_time(text, cooldown)) for text, cooldown in tasks]
    tests += [('100000 1000000000\n' + 'A' * 100000 + '\n', 99999 * 1000000001 + 1)]
    add('task-scheduler', 'Task Scheduler', 'MEDIUM', ['Greedy', 'Counting'],
        'Each task takes one time slot and is identified by an uppercase letter. You may reorder tasks or leave idle slots. Between two executions of the same letter, at least cooldown other slots must pass. Find the fewest slots needed to finish all tasks.',
        '0 ≤ task count ≤ 100,000; 0 ≤ cooldown ≤ 1,000,000,000. The answer may need 64 bits.',
        'First line: task count and cooldown. Second line: the task letters with no spaces, possibly empty.', 'Print the minimum total number of slots.', tests)

    medians = [([1, 3], [2]), ([1, 2], [3, 4]), ([], [5]), ([-1], [0]), ([-5, -3], [-2, -1]), ([0, 0], [0, 0])]
    for _ in range(10):
        first = sorted(RNG.randint(-100, 100) for _ in range(RNG.randint(0, 25)))
        second = sorted(RNG.randint(-100, 100) for _ in range(RNG.randint(1, 25)))
        medians.append((first, second))
    tests = []
    for first, second in medians:
        ordered = sorted(first + second)
        median = (ordered[(len(ordered) - 1) // 2] + ordered[len(ordered) // 2]) / 2
        tests.append((f'{len(first)} {len(second)}\n' + ' '.join(map(str, first)) + '\n' + ' '.join(map(str, second)) + '\n', f'{median:.1f}'))
    tests += [('100000 100000\n' + ('0 ' * 100000 + '\n') * 2, '0.0')]
    add('median-of-two-sorted-arrays', 'Median of Two Sorted Arrays', 'HARD', ['Arrays', 'Binary search'],
        'Find the median of two sorted arrays considered as one sorted sequence. For an even total length, average the two middle values. Aim for logarithmic time rather than merging the full arrays.',
        '0 ≤ n, m ≤ 100,000; n + m ≥ 1; |value| ≤ 1,000,000,000. Both arrays are nondecreasing.',
        'First line: n and m. Second line: n values. Third line: m values. An array line may be empty.',
        'Print the median with exactly one digit after the decimal point, such as 2.0, 2.5, or -0.5.', tests)

    windows = [('ADOBECODEBANC', 'ABC'), ('a', 'aa'), ('', ''), ('abc', ''), ('', 'a'), ('ab', 'b'), ('bbaa', 'aba')]
    windows += [(''.join(RNG.choices('abcABC', k=RNG.randint(0, 25))), ''.join(RNG.choices('abcABC', k=RNG.randint(0, 6)))) for _ in range(10)]
    tests = [(text + '\n' + target + '\n', minimum_window(text, target)) for text, target in windows]
    tests += [('a' * 99998 + 'bc\nabc\n', 'abc')]
    add('minimum-window-substring', 'Minimum Window Substring', 'HARD', ['Strings', 'Sliding window'],
        'Find the shortest contiguous part of the first string containing every character of the second, including repeated occurrences. Matching is case-sensitive. If several shortest windows exist, choose the earliest. An empty target has an empty answer.',
        'Each string has 0 to 100,000 English letters, uppercase or lowercase.',
        'First line: source string. Second line: target string. Either may be empty.', 'Print the shortest window, NONE if impossible, or an empty line for an empty target.', tests)

    texts = ['zzazz', 'mbadm', '', 'a', 'abc', 'leetcode']
    texts += [''.join(RNG.choices('abcd', k=RNG.randint(0, 20))) for _ in range(10)]
    tests = [(text + '\n', len(text) - longest_common_subsequence(text, text[::-1])) for text in texts]
    tests += [('ab' * 1000 + '\n', 1)]
    add('minimum-insertions-palindrome', 'Minimum Insertions for a Palindrome', 'HARD', ['Strings', 'Dynamic programming'],
        'Insert as few characters as possible anywhere in a string so that it becomes a palindrome. You cannot delete, replace, or rearrange existing characters.',
        '0 ≤ length ≤ 2,000. Characters are lowercase English letters.', 'One line containing the string, possibly empty.',
        'Print the minimum number of insertions.', tests)

    balloons = [[3, 1, 5, 8], [1, 5], [], [0], [7], [0, 2, 0, 3]]
    balloons += [[RNG.randint(0, 8) for _ in range(RNG.randint(0, 7))] for _ in range(10)]
    tests = [(array_input(values), burst_balloons(values)) for values in balloons]
    tests += [(array_input([1000] * 200), 198001001000)]
    add('burst-balloons', 'Burst Balloons', 'HARD', ['Arrays', 'Interval dynamic programming'],
        'Burst every balloon in the best order. Bursting a balloon earns its value times the values of its nearest remaining neighbors. A missing neighbor beyond either end has value 1. After a balloon is removed, its remaining neighbors become adjacent.',
        '0 ≤ n ≤ 200; 0 ≤ balloon value ≤ 1,000. The score may need 64 bits.',
        'First line: n. Second line: n balloon values.', 'Print the greatest total score.', tests)

    texts = ['aab', 'a', '', 'ab', 'abba', 'abccbc']
    texts += [''.join(RNG.choices('abc', k=RNG.randint(0, 18))) for _ in range(10)]
    tests = [(text + '\n', palindrome_cuts(text)) for text in texts]
    tests += [('a' * 1999 + 'b\n', 1)]
    add('palindrome-partitioning-min-cuts', 'Palindrome Partitioning: Minimum Cuts', 'HARD', ['Strings', 'Dynamic programming'],
        'Cut a string into contiguous pieces so that every piece is a palindrome. Find the smallest number of cuts. A string that is already a palindrome needs zero cuts, as does an empty string.',
        '0 ≤ length ≤ 2,000. Characters are lowercase English letters.', 'One line containing the string, possibly empty.',
        'Print the minimum cut count.', tests)

    matrices = [[[9, 9, 4], [6, 6, 8], [2, 1, 1]], [[3, 4, 5], [3, 2, 6], [2, 2, 1]], [[7]], [[1, 1], [1, 1]]]
    for _ in range(10):
        rows, columns = RNG.randint(1, 5), RNG.randint(1, 5)
        matrices.append([[RNG.randint(-10, 10) for _ in range(columns)] for _ in range(rows)])
    tests = [(matrix_input(matrix), increasing_path(matrix)) for matrix in matrices]
    snake = [list(range(row * 200, (row + 1) * 200)) for row in range(200)]
    for row in range(1, 200, 2):
        snake[row].reverse()
    tests += [(matrix_input(snake), 40000)]
    add('longest-increasing-path-matrix', 'Longest Increasing Path in a Matrix', 'HARD', ['Matrices', 'Graphs', 'Topological sorting'],
        'Start at any matrix cell and move horizontally or vertically to a strictly larger value. Find the greatest number of cells in such a path. Equal values cannot extend it, and diagonal moves are forbidden.',
        '1 ≤ rows, columns ≤ 200; |value| ≤ 1,000,000,000.', 'First line: rows and columns. Then the matrix rows.',
        'Print the longest increasing path length.', tests)

    windows = [([2, -1, 2], 3), ([1, 2], 4), ([], 1), ([-5, 10, -3], 7), ([1], 1)]
    windows += [([RNG.randint(-15, 15) for _ in range(RNG.randint(0, 35))], RNG.randint(1, 60)) for _ in range(10)]
    tests = [(f'{len(values)} {target}\n' + ' '.join(map(str, values)) + '\n',
              min((end - start for start in range(len(values)) for end in range(start + 1, len(values) + 1)
                   if sum(values[start:end]) >= target), default=-1)) for values, target in windows]
    tests += [('100000 50000000000000\n' + '1000000000 ' * 100000 + '\n', 50000)]
    add('shortest-subarray-at-least-k', 'Shortest Subarray With Sum at Least K', 'HARD', ['Arrays', 'Prefix sums', 'Monotonic queues'],
        'Find the shortest nonempty contiguous part whose sum is at least the target. Values may be negative, so an ordinary positive-number sliding window is not sufficient.',
        '0 ≤ n ≤ 100,000; |value| ≤ 1,000,000,000; 1 ≤ target ≤ 10^18.',
        'First line: n and target. Second line: n integers.', 'Print the minimum length, or -1 if no part qualifies.', tests)

    arrays = [[5, 2, 6, 1], [-1, -1], [], [1], [3, 2, 1], [1, 2, 3]]
    arrays += [[RNG.randint(-20, 20) for _ in range(RNG.randint(0, 40))] for _ in range(10)]
    tests = [(array_input(values), ' '.join(str(sum(later < value for later in values[index + 1:]))
              for index, value in enumerate(values))) for values in arrays]
    tests += [(array_input(list(range(10000, 0, -1))), ' '.join(map(str, range(9999, -1, -1))))]
    add('count-smaller-after-self', 'Count Smaller Numbers After Self', 'HARD', ['Arrays', 'Fenwick trees', 'Sorting'],
        'For every array position, count how many later positions contain a strictly smaller value. Equal values do not count. Return the counts in the original position order.',
        '0 ≤ n ≤ 10,000; |value| ≤ 1,000,000,000.', 'First line: n. Second line: n integers.',
        'Print one count per position on one line, or an empty line if the array is empty.', tests)

    prices = [[3, 3, 5, 0, 0, 3, 1, 4], [1, 2, 3, 4, 5], [], [9], [7, 6, 4, 3, 1]]
    prices += [[RNG.randint(0, 20) for _ in range(RNG.randint(0, 12))] for _ in range(10)]
    tests = [(array_input(values), two_trades(values)) for values in prices]
    tests += [(array_input([0, 1000000000] * 50000), 2000000000)]
    add('stock-profit-two-transactions', 'Stock Profit With Two Transactions', 'HARD', ['Arrays', 'Dynamic programming'],
        'A stock price is given for each day. Make at most two buy-then-sell transactions to maximize profit. You must sell before buying again and may hold at most one share. Making no trade is allowed.',
        '0 ≤ n ≤ 100,000; 0 ≤ price ≤ 1,000,000,000.', 'First line: n. Second line: n prices in day order.',
        'Print the maximum profit with at most two transactions.', tests)

    patterns = [('aa', 'a'), ('aa', 'a*'), ('', ''), ('', 'a*'), ('', 'a'), ('ab', '.*'), ('aab', 'c*a*b'), ('mississippi', 'mis*is*p*.')]
    for _ in range(10):
        text = ''.join(RNG.choices('ab', k=RNG.randint(0, 12)))
        pattern = ''.join(RNG.choice('ab.') + ('*' if RNG.randrange(2) else '') for _ in range(RNG.randint(0, 6)))
        patterns.append((text, pattern))
    tests = [(text + '\n' + pattern + '\n', 'YES' if re.fullmatch(pattern, text) is not None else 'NO') for text, pattern in patterns]
    tests += [('a' * 2000 + '\n' + 'a*' * 1000 + '\n', 'YES')]
    add('regular-expression-matching', 'Regular Expression Matching', 'HARD', ['Strings', 'Dynamic programming'],
        'Match the entire string against a pattern. A lowercase letter matches itself, a dot matches any single letter, and * means zero or more copies of the preceding letter or dot. Matching only part of the string is not enough.',
        'String and pattern lengths are 0 to 2,000. The string uses lowercase English letters. The pattern uses lowercase letters, dots, and stars; a star never appears first or immediately after another star.',
        'First line: the string. Second line: the pattern. Either may be empty.', 'Print YES for a complete match, otherwise NO.', tests)

    return result
