"""Publish typed function contracts as new versions while preserving the stdin versions."""
from copy import deepcopy
from pathlib import Path


ROOT = Path(__file__).resolve().parent

SPECS = {
    'balanced-relay': ('String', [('signal', 'String')], 'line'),
    'signal-window': ('int', [('signal', 'String')], 'line'),
    'cargo-pairs': ('long', [('weights', 'int[]'), ('target', 'long')], 'array-target'),
    'coverage-line': ('long', [('intervals', 'long[][]')], 'pairs'),
    'dock-slots': ('int', [('reservations', 'int[][]')], 'pairs'),
    'grid-courier': ('int', [('grid', 'String[]')], 'grid'),
    'target-bursts': ('long', [('values', 'int[]'), ('target', 'long')], 'array-target'),
    'compass-picks': ('int[]', [('points', 'int[][]'), ('k', 'int')], 'points'),
    'token-mint': ('int', [('coins', 'int[]'), ('target', 'int')], 'array-target'),
    'rising-antennas': ('int', [('heights', 'int[]')], 'array'),
    'archive-lookup': ('int[]', [('keys', 'int[]'), ('queries', 'int[]')], 'two-arrays'),
    'island-ledger': ('int[]', [('grid', 'String[]')], 'grid'),
    'turned-ledger': ('int', [('keys', 'int[]')], 'array'),
    'equal-banner': ('int', [('tiles', 'int[]')], 'array'),
    'step-bridges': ('int', [('n', 'int'), ('broken', 'int[]')], 'stones'),
    'two-sum': ('int[]', [('nums', 'int[]'), ('target', 'long')], 'array-target'),
    'palindrome-check': ('boolean', [('text', 'String')], 'line'),
    'valid-anagram': ('boolean', [('first', 'String'), ('second', 'String')], 'two-lines'),
    'best-stock-profit': ('long', [('prices', 'int[]')], 'array'),
    'contains-duplicate': ('boolean', [('nums', 'int[]')], 'array'),
    'missing-number': ('int', [('nums', 'int[]')], 'array'),
    'move-zeroes': ('int[]', [('nums', 'int[]')], 'array'),
    'single-number': ('int', [('nums', 'int[]')], 'array'),
    'majority-element': ('int', [('nums', 'int[]')], 'array'),
    'range-sum-query': ('long[]', [('nums', 'int[]'), ('queries', 'int[][]')], 'ranges'),
    'gcd-and-lcm': ('long[]', [('a', 'long'), ('b', 'long')], 'numbers'),
    'merge-sorted-arrays': ('int[]', [('first', 'int[]'), ('second', 'int[]')], 'two-arrays'),
    'maximum-subarray': ('long', [('nums', 'int[]')], 'array'),
    'next-permutation': ('int[]', [('nums', 'int[]')], 'array'),
    'container-with-most-water': ('long', [('heights', 'int[]')], 'array'),
    'daily-temperatures': ('int[]', [('temperatures', 'int[]')], 'array'),
    'search-rotated-array': ('int', [('nums', 'int[]'), ('target', 'int')], 'array-target'),
    'jump-game': ('boolean', [('jumps', 'int[]')], 'array'),
    'house-robber': ('long', [('amounts', 'int[]')], 'array'),
    'word-break': ('boolean', [('text', 'String'), ('words', 'String[]')], 'dictionary'),
    'edit-distance': ('int', [('first', 'String'), ('second', 'String')], 'two-lines'),
    'longest-common-subsequence': ('int', [('first', 'String'), ('second', 'String')], 'two-lines'),
    'shortest-path': ('long', [('n', 'int'), ('edges', 'int[][]'), ('source', 'int'), ('target', 'int')], 'route'),
    'course-schedule': ('boolean', [('n', 'int'), ('prerequisites', 'int[][]')], 'edges'),
    'trapping-rain-water': ('long', [('heights', 'int[]')], 'array'),
    'sliding-window-maximum': ('int[]', [('nums', 'int[]'), ('k', 'int')], 'array-target'),
    'largest-rectangle-histogram': ('long', [('heights', 'int[]')], 'array'),
    'longest-valid-parentheses': ('int', [('text', 'String')], 'line'),
    'minimum-spanning-tree': ('String', [('n', 'int'), ('edges', 'int[][]')], 'weighted-edges'),
    'zero-one-knapsack': ('long', [('weights', 'int[]'), ('values', 'int[]'), ('capacity', 'int')], 'knapsack'),
    'fizz-buzz': ('String[]', [('n', 'int')], 'numbers'),
    'factorial-trailing-zeroes': ('long', [('n', 'long')], 'numbers'),
    'count-set-bits': ('int', [('n', 'long')], 'numbers'),
    'power-of-two': ('boolean', [('n', 'long')], 'numbers'),
    'reverse-integer': ('int', [('n', 'int')], 'numbers'),
    'integer-square-root': ('long', [('n', 'long')], 'numbers'),
    'excel-column-number': ('long', [('column', 'String')], 'line'),
    'roman-to-integer': ('int', [('numeral', 'String')], 'line'),
    'longest-common-prefix': ('String', [('words', 'String[]')], 'word-list'),
    'first-unique-character': ('int', [('text', 'String')], 'line'),
    'is-subsequence': ('boolean', [('subsequence', 'String'), ('text', 'String')], 'two-lines'),
    'isomorphic-strings': ('boolean', [('first', 'String'), ('second', 'String')], 'two-lines'),
    'matrix-diagonal-sum': ('long', [('matrix', 'int[][]')], 'square'),
    'transpose-matrix': ('int[][]', [('matrix', 'int[][]')], 'matrix'),
    'sorted-squares': ('long[]', [('nums', 'int[]')], 'array'),
    'plus-one': ('int[]', [('digits', 'int[]')], 'array'),
    'add-binary': ('String', [('first', 'String'), ('second', 'String')], 'two-lines'),
    'ransom-note': ('boolean', [('note', 'String'), ('letters', 'String')], 'two-lines'),
    'length-of-last-word': ('int', [('text', 'String')], 'line'),
    'pascals-triangle-row': ('long[]', [('k', 'int')], 'numbers'),
    'three-sum': ('long', [('nums', 'int[]')], 'array'),
    'four-sum-count': ('long', [('a', 'int[]'), ('b', 'int[]'), ('c', 'int[]'), ('d', 'int[]')], 'four-arrays'),
    'longest-consecutive-sequence': ('int', [('nums', 'int[]')], 'array'),
    'product-except-self': ('long[]', [('nums', 'int[]')], 'array'),
    'minimum-size-subarray-sum': ('int', [('nums', 'int[]'), ('target', 'long')], 'array-target'),
    'character-replacement': ('int', [('text', 'String'), ('k', 'int')], 'replacements'),
    'subarray-product-less-than-k': ('long', [('nums', 'int[]'), ('k', 'long')], 'array-target'),
    'partition-equal-subset-sum': ('boolean', [('nums', 'int[]')], 'array'),
    'coin-change-combinations': ('int', [('coins', 'int[]'), ('target', 'int')], 'array-target'),
    'unique-paths-with-obstacles': ('int', [('grid', 'String[]')], 'grid'),
    'minimum-path-sum': ('long', [('grid', 'int[][]')], 'matrix'),
    'triangle-minimum-path-sum': ('long', [('triangle', 'int[][]')], 'triangle'),
    'minimum-falling-path-sum': ('long', [('matrix', 'int[][]')], 'square'),
    'longest-palindromic-substring': ('int', [('text', 'String')], 'line'),
    'decode-ways': ('int', [('digits', 'String')], 'line'),
    'evaluate-reverse-polish-notation': ('long', [('tokens', 'String[]')], 'tokens'),
    'simplify-unix-path': ('String', [('path', 'String')], 'line'),
    'top-k-frequent-elements': ('int[]', [('nums', 'int[]'), ('k', 'int')], 'array-target'),
    'kth-largest-element': ('int', [('nums', 'int[]'), ('k', 'int')], 'array-target'),
    'kth-smallest-sorted-matrix': ('int', [('matrix', 'int[][]'), ('k', 'int')], 'ranked-square'),
    'spiral-matrix': ('int[]', [('matrix', 'int[][]')], 'matrix'),
    'valid-sudoku': ('boolean', [('board', 'String[]')], 'board'),
    'word-search': ('boolean', [('board', 'String[]'), ('word', 'String')], 'word-search'),
    'non-overlapping-intervals': ('int', [('intervals', 'int[][]')], 'pairs'),
    'task-scheduler': ('long', [('tasks', 'String'), ('cooldown', 'long')], 'tasks'),
    'median-of-two-sorted-arrays': ('double', [('first', 'int[]'), ('second', 'int[]')], 'two-arrays'),
    'minimum-window-substring': ('String', [('text', 'String'), ('target', 'String')], 'two-lines'),
    'minimum-insertions-palindrome': ('int', [('text', 'String')], 'line'),
    'burst-balloons': ('long', [('nums', 'int[]')], 'array'),
    'palindrome-partitioning-min-cuts': ('int', [('text', 'String')], 'line'),
    'longest-increasing-path-matrix': ('int', [('matrix', 'int[][]')], 'matrix'),
    'shortest-subarray-at-least-k': ('int', [('nums', 'int[]'), ('target', 'long')], 'array-target'),
    'count-smaller-after-self': ('int[]', [('nums', 'int[]')], 'array'),
    'stock-profit-two-transactions': ('long', [('prices', 'int[]')], 'array'),
    'regular-expression-matching': ('boolean', [('text', 'String'), ('pattern', 'String')], 'two-lines'),
}


def decode_arguments(data, layout):
    text_lines = data.split('\n')
    if layout == 'line':
        return [text_lines[0]]
    if layout == 'two-lines':
        return text_lines[:2]
    if layout == 'word-list':
        return [text_lines[1:int(text_lines[0]) + 1]]
    if layout == 'dictionary':
        return [text_lines[0], text_lines[2:int(text_lines[1]) + 2]]
    if layout == 'board':
        return [text_lines[:9]]
    if layout in ('grid', 'word-search'):
        rows, columns = map(int, text_lines[0].split())
        grid = text_lines[1:rows + 1]
        assert all(len(row) == columns for row in grid)
        return [grid, text_lines[rows + 1]] if layout == 'word-search' else [grid]
    if layout == 'tokens':
        return [data.split()[1:]]
    if layout == 'replacements':
        return [text_lines[1], int(text_lines[0])]
    if layout == 'tasks':
        return [text_lines[1], int(text_lines[0].split()[1])]

    values = list(map(int, data.split()))
    if layout == 'numbers':
        return values
    count = values[0]
    if layout == 'array':
        return [values[1:count + 1]]
    if layout == 'array-target':
        return [values[2:count + 2], values[1]]
    if layout == 'two-arrays':
        second = values[1]
        return [values[2:count + 2], values[count + 2:count + second + 2]]
    if layout == 'four-arrays':
        return [values[1 + count * group:1 + count * (group + 1)] for group in range(4)]
    if layout in ('pairs', 'points', 'edges', 'weighted-edges', 'route'):
        header = {'pairs': 1, 'points': 2, 'edges': 2, 'weighted-edges': 2, 'route': 4}[layout]
        width = 3 if layout in ('weighted-edges', 'route') else 2
        items = [values[index:index + width] for index in range(header, len(values), width)]
        if layout == 'pairs':
            return [items]
        if layout == 'points':
            return [items, values[1]]
        if layout == 'route':
            return [count, items, values[2], values[3]]
        return [count, items]
    if layout == 'ranges':
        numbers = values[2:count + 2]
        queries = [values[index:index + 2] for index in range(count + 2, len(values), 2)]
        return [numbers, queries]
    if layout == 'stones':
        return [count, values[2:]]
    if layout == 'knapsack':
        return [values[2::2], values[3::2], values[1]]
    if layout == 'triangle':
        return [[list(map(int, line.split())) for line in text_lines[1:count + 1]]]
    if layout in ('matrix', 'square', 'ranked-square'):
        columns = count if layout != 'matrix' else values[1]
        header = 1 if layout == 'square' else 2
        matrix = [values[header + row * columns:header + (row + 1) * columns] for row in range(count)]
        return [matrix, values[1]] if layout == 'ranked-square' else [matrix]
    raise ValueError(f'Unknown input layout: {layout}')


def decode_expected(slug, data, result_type):
    output = data.strip()
    if slug == 'two-sum' and output == '-1':
        return []
    if slug == 'minimum-window-substring' and output == 'NONE':
        return ''
    if result_type == 'boolean':
        assert output in ('YES', 'NO'), slug
        return output == 'YES'
    if result_type == 'String':
        return output
    if result_type == 'String[]':
        return output.split()
    if result_type == 'int[][]':
        return [list(map(int, line.split())) for line in output.splitlines()]
    if result_type.endswith('[]'):
        return list(map(int, output.split()))
    return float(output) if result_type == 'double' else int(output)


def starter(result_type, parameters):
    if result_type.endswith('[]'):
        base = result_type[:-2]
        default = f'new {base}[0]' if not base.endswith('[]') else f'new {base[:-2]}[0][]'
    else:
        default = {'String': '""', 'boolean': 'false', 'long': '0L', 'double': '0.0', 'int': '0'}[result_type]
    arguments = ', '.join(f'{kind} {name}' for name, kind in parameters)
    return ('import java.util.*;\n\npublic class Solution {\n'
            f'  public {result_type} solve({arguments}) {{\n    return {default};\n  }}\n}}\n')


def build_functions(legacy):
    result = []
    assert set(SPECS) == {content['slug'] for content in legacy}
    for original in legacy:
        slug = original['slug']
        result_type, parameters, layout = SPECS[slug]
        content = deepcopy(original)
        content['version'] = 2
        content['function'] = dict(returnType=result_type,
                                   parameters=[dict(name=name, type=kind) for name, kind in parameters])
        content['starterCode'] = starter(result_type, parameters)
        content['referenceSource'] = (ROOT / slug / 'Solution.java').read_text(encoding='utf-8')
        content['inputFormat'] = 'Arguments: ' + ', '.join(f'{name} ({kind})' for name, kind in parameters) + '.'
        if layout in ('array', 'array-target'):
            content['inputFormat'] += f' n is {parameters[0][0]}.length.'
        if layout == 'two-arrays':
            content['inputFormat'] += ' n and m are the lengths of the first and second arrays.'
        content['outputFormat'] = original['outputFormat'].replace('Print', 'Return').replace('print', 'return')
        if result_type == 'boolean':
            content['outputFormat'] = content['outputFormat'].replace('YES', 'true').replace('NO', 'false')
        elif result_type.endswith('[]'):
            content['outputFormat'] = f'Return the result as {result_type}, in the order described above. Return an empty array when there are no results.'
        overrides = {
            'two-sum': 'Return the 1-based pair [i, j], with i < j and the smallest first position, then second position. Return [] if no pair exists.',
            'island-ledger': 'Return [islandCount, largestIslandSize]. An all-water map returns [0, 0].',
            'gcd-and-lcm': 'Return [greatestCommonDivisor, leastCommonMultiple] as a long[].',
            'range-sum-query': 'Return one sum for each inclusive 1-based query, keeping the query order.',
            'median-of-two-sorted-arrays': 'Return the median as a double.',
            'minimum-spanning-tree': 'Return the total weight as a decimal string, or "IMPOSSIBLE" if the graph is disconnected.',
            'minimum-window-substring': 'Return the shortest window, choosing the earliest on ties. Return an empty string if no window exists or the target is empty.',
            'transpose-matrix': 'Return the transposed matrix as int[][].',
            'fizz-buzz': 'Return the n results in order as a String[].',
        }
        content['outputFormat'] = overrides.get(slug, content['outputFormat'])
        content['cases'] = []
        for test in original['cases']:
            values = decode_arguments(test['input'], layout)
            assert len(values) == len(parameters), slug
            content['cases'].append(dict(arguments={name: value for (name, kind), value in zip(parameters, values)},
                                         expected=decode_expected(slug, test['output'], result_type), public=test['public']))
        result.append(content)
    return result
