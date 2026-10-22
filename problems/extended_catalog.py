"""Original v1 challenges, independent Python oracles and deterministic edge/random cases."""
from bisect import bisect_left
from collections import deque
from itertools import product
import random

RNG = random.Random(20261021)


def build_extra(problem, case):
    result = []
    def add(slug, title, difficulty, tags, statement, constraints, input_format, output_format, tests):
        result.append(problem(slug, title, difficulty, tags, statement, constraints, input_format,
                              output_format, [case(data, str(answer), i < 2) for i, (data, answer) in enumerate(tests)]))
    def intervals_input(intervals):
        return f'{len(intervals)}\n' + ''.join(f'{a} {b}\n' for a, b in intervals)
    def array_input(values):
        return f'{len(values)}\n' + ' '.join(map(str, values)) + '\n'
    def grid_input(grid):
        return f'{len(grid)} {len(grid[0])}\n' + '\n'.join(grid) + '\n'

    intervals = [[(0, 4), (2, 8), (10, 12)], [(1, 2), (2, 3)], [], [(4, 8)], [(-4, 5), (-2, 2), (-4, 5)]]
    intervals += [[(a := RNG.randint(-20, 20), a + RNG.randint(1, 12)) for _ in range(RNG.randint(0, 20))] for _ in range(16)]
    coverage = [(intervals_input(items), len({x for a, b in items for x in range(a, b)})) for items in intervals]
    coverage += [(intervals_input([(-10**12, 10**12)]), 2 * 10**12),
                 (intervals_input([(i * 3, i * 3 + 1) for i in range(100000)]), 100000)]
    add('coverage-line', 'Coverage Line', 'MEDIUM', ['Sorting', 'Intervals'],
        'A maintenance drone paints several stretches of a straight rail. Stretch [a, b) includes a and stops just before b. Overlapping paint counts only once. Find the total rail length covered after all stretches have been painted; their arrival order has no significance.',
        '0 ≤ n ≤ 100,000; −10^12 ≤ a < b ≤ 10^12. Endpoints are integers. Use a 64-bit answer.',
        'The first line contains n. Each of the following n lines contains a and b.',
        'Print the total covered length.', coverage)

    visits = [[(0, 5), (2, 7), (5, 8)], [(1, 2), (2, 3), (3, 4)], [], [(2, 8)] * 6]
    visits += [[(a := RNG.randint(0, 30), a + RNG.randint(1, 10)) for _ in range(RNG.randint(0, 35))] for _ in range(16)]
    dock = []
    for items in visits:
        times = sorted({x for item in items for x in item})
        dock.append((intervals_input(items), max([sum(a <= time < b for a, b in items) for time in times] or [0])))
    dock += [(intervals_input([(0, 10)] * 100000), 100000)]
    add('dock-slots', 'Dock Slots', 'MEDIUM', ['Heaps', 'Sorting'],
        'Delivery pods reserve one docking slot from their arrival time up to, but excluding, their departure time. A slot can be reused exactly when its previous pod departs. Determine the smallest number of slots that lets every reservation proceed without waiting.',
        '0 ≤ n ≤ 100,000; 0 ≤ arrival < departure ≤ 1,000,000,000.',
        'First line: n. Then n lines, each containing arrival and departure.',
        'Print the minimum number of slots. With no reservations, print 0.', dock)

    def route(grid):
        h, w = len(grid), len(grid[0])
        if grid[0][0] == '#' or grid[-1][-1] == '#': return -1
        queue, seen = deque([(0, 0, 0)]), {(0, 0)}
        while queue:
            r, c, d = queue.popleft()
            if (r, c) == (h-1, w-1): return d
            for nr, nc in ((r-1,c),(r+1,c),(r,c-1),(r,c+1)):
                if 0 <= nr < h and 0 <= nc < w and grid[nr][nc] != '#' and (nr,nc) not in seen:
                    seen.add((nr,nc)); queue.append((nr,nc,d+1))
        return -1
    grids = [['...', '.#.', '...'], ['.#', '#.'], ['.'], ['#']]
    grids += [[''.join(bits[:2]), ''.join(bits[2:])] for bits in product('.#', repeat=4)]
    grids += [[''.join(RNG.choices('.#', weights=[3,1], k=8)) for _ in range(7)] for _ in range(12)]
    grids += [['.' * 500] * 500]
    add('grid-courier', 'Grid Courier', 'MEDIUM', ['BFS', 'Graphs'],
        'A courier starts in the top-left cell of a rectangular warehouse and must reach the bottom-right cell. Each move enters a horizontally or vertically adjacent open cell. Walls cannot be crossed. Find the fewest moves; a blocked start or destination makes the route impossible.',
        '1 ≤ rows, columns ≤ 500. A dot is an open cell and # is a wall.',
        'First line: rows and columns. Then rows strings of exactly columns cells.',
        'Print the fewest moves, or -1 if no route exists.', [(grid_input(g), route(g)) for g in grids])

    bursts = [([2,-1,2,1], 3), ([0,0,0], 0), ([], 4), ([-2,-2,2], -2)]
    bursts += [([RNG.randint(-5,5) for _ in range(RNG.randint(0,60))], RNG.randint(-12,12)) for _ in range(20)]
    burst_tests = []
    for values, target in bursts:
        count = 0
        for start in range(len(values)):
            total = 0
            for end in range(start, len(values)):
                total += values[end]; count += total == target
        burst_tests.append((f'{len(values)} {target}\n' + ' '.join(map(str, values)) + '\n', count))
    burst_tests.append(('200000 0\n' + '0 ' * 200000 + '\n', 20000100000))
    add('target-bursts', 'Target Bursts', 'MEDIUM', ['Prefix sums', 'Hash maps'],
        'A sensor reports signed adjustments in chronological order. A burst is a nonempty contiguous sequence of reports. Count the bursts whose adjustments add to the requested target. Negative adjustments are valid, and bursts may overlap.',
        '0 ≤ n ≤ 200,000; |adjustment| ≤ 1,000,000; |target| ≤ 1,000,000,000. The count needs 64 bits.',
        'First line: n and target. Second line: n adjustments, empty if n is 0.',
        'Print the number of target-sum bursts.', burst_tests)

    points = [([(3,4),(0,2),(-2,0),(0,0)], 3), ([(1,0),(0,1),(-1,0)], 2), ([],0)]
    for _ in range(20):
        n = RNG.randint(1,45)
        points.append(([(RNG.randint(-20,20), RNG.randint(-20,20)) for _ in range(n)], RNG.randint(0,n)))
    points += [([(1000000,-1000000),(-1000000,1000000),(999999,1000000)],3)]
    point_tests = []
    for items,k in points:
        order = sorted(range(len(items)), key=lambda i: (items[i][0]**2 + items[i][1]**2, i))[:k]
        point_tests.append((f'{len(items)} {k}\n' + ''.join(f'{x} {y}\n' for x,y in items), ' '.join(str(i+1) for i in order)))
    add('compass-picks', 'Compass Picks', 'EASY', ['Sorting', 'Geometry'],
        'A survey team can visit k of its numbered stations. Choose those closest to the origin. Compare squared Euclidean distance; when two stations are equally distant, the smaller original station number comes first. Report the chosen station numbers in that order.',
        '0 ≤ k ≤ n ≤ 100,000; |x|, |y| ≤ 1,000,000. Station numbers are 1-based. Use 64-bit squared distances.',
        'First line: n and k. Then n lines containing x and y for stations 1 through n.',
        'Print k station numbers ordered by distance, breaking ties by station number. Print an empty line when k is 0.', point_tests)

    def mint(coins, target):
        queue, distances = deque([0]), {0:0}
        while queue:
            value = queue.popleft()
            if value == target: return distances[value]
            for coin in coins:
                next_value = value + coin
                if next_value <= target and next_value not in distances:
                    distances[next_value] = distances[value] + 1; queue.append(next_value)
        return -1
    mints = [([1,4,6],8), ([4,6],7), ([5],0), ([],8), ([],0)]
    mints += [(RNG.sample(range(1,15), RNG.randint(1,7)), RNG.randint(0,100)) for _ in range(18)]
    mints += [([1,97,251],100000)]
    add('token-mint', 'Token Mint', 'MEDIUM', ['Dynamic programming', 'Arrays'],
        'A workshop has unlimited tokens in the listed positive denominations. Assemble exactly the requested value using as few tokens as possible. A denomination can be reused; if exact assembly is impossible, report failure.',
        '0 ≤ denomination count ≤ 100; 0 ≤ target ≤ 100,000; distinct denominations are between 1 and 10,000.',
        'First line: denomination count and target. Second line: the denominations, possibly empty.',
        'Print the minimum token count, or -1 when impossible. Value 0 requires no tokens.',
        [(f'{len(coins)} {target}\n' + ' '.join(map(str,coins)) + '\n', mint(coins,target)) for coins,target in mints])

    sequences = [[5,1,4,2,3,6], [7,7,7], [], [1], [-5,-2,-4,0]]
    sequences += [[RNG.randint(-15,15) for _ in range(RNG.randint(0,80))] for _ in range(20)]
    lis_tests = []
    for values in sequences:
        best = [1] * len(values)
        for i in range(len(values)):
            best[i] = 1 + max([best[j] for j in range(i) if values[j] < values[i]] or [0])
        lis_tests.append((array_input(values), max(best or [0])))
    lis_tests += [(array_input(list(range(200000))),200000), (array_input(list(range(200000,0,-1))),1)]
    add('rising-antennas', 'Rising Antennas', 'HARD', ['Binary search', 'Dynamic programming'],
        'Antennas stand in a fixed line. Select some of them without changing their order so that each selected height is strictly greater than the previous selected height. Skipping antennas is allowed. Find the greatest possible number selected.',
        '0 ≤ n ≤ 200,000; heights are signed 32-bit integers. Equal heights cannot extend a strictly rising sequence.',
        'First line: n. Second line: n heights, empty if n is 0.',
        'Print the length of the longest strictly increasing subsequence.', lis_tests)

    archives = [([2,2,5,9],[2,3,10,-1]), ([],[0,4]), ([5],[5,6,4])]
    archives += [(sorted(RNG.randint(-30,30) for _ in range(RNG.randint(0,70))), [RNG.randint(-35,35) for _ in range(20)]) for _ in range(20)]
    archive_tests = []
    for values,queries in archives:
        answers = [bisect_left(values,q) for q in queries]
        archive_tests.append((f'{len(values)} {len(queries)}\n' + ' '.join(map(str,values)) + '\n' + ' '.join(map(str,queries)) + '\n', '\n'.join(str(-1 if i == len(values) else i) for i in answers)))
    add('archive-lookup', 'Archive Lookup', 'EASY', ['Binary search', 'Arrays'],
        'An archive stores nondecreasing integer keys, including duplicates. For each query, find the first position whose stored key is at least the requested key. Positions are zero-based. A query beyond every stored key has no answer.',
        '0 ≤ n ≤ 200,000; 1 ≤ queries ≤ 200,000; keys and requests are signed 32-bit integers.',
        'First line: n and query count. Second line: n sorted keys. Third line: the query values.',
        'For each query, print its first qualifying zero-based position, or -1. Print answers in query order.', archive_tests)

    def islands(grid):
        visited, count, largest = set(), 0, 0
        for r in range(len(grid)):
            for c in range(len(grid[0])):
                if grid[r][c] != '1' or (r,c) in visited: continue
                count += 1; pending, size = [(r,c)], 0; visited.add((r,c))
                while pending:
                    y,x = pending.pop(); size += 1
                    for ny,nx in ((y-1,x),(y+1,x),(y,x-1),(y,x+1)):
                        if 0 <= ny < len(grid) and 0 <= nx < len(grid[0]) and grid[ny][nx] == '1' and (ny,nx) not in visited:
                            visited.add((ny,nx)); pending.append((ny,nx))
                largest = max(largest,size)
        return f'{count} {largest}'
    maps = [['1100','0101','0011'], ['10','01'], ['0'], ['1']]
    maps += [[''.join(RNG.choices('01', k=9)) for _ in range(8)] for _ in range(20)]
    maps += [['1' * 500] * 500]
    add('island-ledger', 'Island Ledger', 'MEDIUM', ['DFS', 'Disjoint sets'],
        'A coastal chart marks land as 1 and water as 0. Land cells belong to the same island when a chain of horizontal or vertical land neighbors connects them. Diagonal contact alone does not connect islands. Count the islands and measure the largest one.',
        '1 ≤ rows, columns ≤ 500. An island size is its number of land cells.',
        'First line: rows and columns. Then rows strings of 0 and 1.',
        'Print island count and largest island size, separated by a space. An all-water map produces 0 0.', [(grid_input(g), islands(g)) for g in maps])

    turns = [[4,8,12,1,3], [-3,0,2], [5], [2,1]]
    for _ in range(22):
        values = sorted(RNG.sample(range(-200,200), RNG.randint(1,100)))
        cut = RNG.randrange(len(values)); turns.append(values[cut:] + values[:cut])
    add('turned-ledger', 'Turned Ledger', 'MEDIUM', ['Binary search', 'Arrays'],
        'A ledger originally listed distinct keys in increasing order. Someone moved a possibly empty prefix to the end, preserving the order inside both parts. Locate the smallest key in the resulting list. The list may already be sorted.',
        '1 ≤ n ≤ 200,000. Keys are distinct signed 32-bit integers. Aim for logarithmic search time.',
        'First line: n. Second line: the rotated list of n keys.',
        'Print the zero-based position of the smallest key.', [(array_input(v),v.index(min(v))) for v in turns])

    banners = [[0,1,0,0,1,1,1], [1,1], [], [0,1], [0]]
    banners += [[RNG.randint(0,1) for _ in range(RNG.randint(0,80))] for _ in range(20)]
    banner_tests = []
    for values in banners:
        best = 0
        for start in range(len(values)):
            balance = 0
            for end in range(start,len(values)):
                balance += 1 if values[end] else -1
                if balance == 0: best = max(best,end-start+1)
        banner_tests.append((array_input(values),best))
    banner_tests += [(array_input([0,1] * 100000),200000), (array_input([1] * 200000),0)]
    add('equal-banner', 'Equal Banner', 'MEDIUM', ['Prefix sums', 'Hash maps'],
        'A banner is woven from tiles labeled 0 or 1. Choose one contiguous piece containing equally many tiles of each label. Find the longest such piece; you may remove tiles from the ends but cannot reorder or remove tiles inside the piece.',
        '0 ≤ n ≤ 200,000. Each tile is 0 or 1.',
        'First line: n. Second line: n tile labels, empty if n is 0.',
        'Print the greatest balanced-piece length, or 0 if no nonempty piece qualifies.', banner_tests)

    def paths(destination, blocked):
        def walk(position):
            if position in blocked or position > destination: return 0
            if position == destination: return 1
            return walk(position+1) + walk(position+2)
        return walk(0)
    bridges = [(5,{3}), (4,{1,2}), (0,set()), (1,{1}), (2,set())]
    bridges += [(n := RNG.randint(1,18), set(RNG.sample(range(1,n+1), RNG.randint(0,n)))) for _ in range(20)]
    bridge_tests = [(f'{n} {len(blocked)}\n' + ' '.join(map(str,sorted(blocked))) + '\n', paths(n,blocked)) for n,blocked in bridges]
    a,b = 1,0
    for _ in range(200000): a,b = (a+b) % 1000000007,a
    bridge_tests.append(('200000 0\n\n',a))
    add('step-bridges', 'Step Bridges', 'EASY', ['Dynamic programming', 'Counting'],
        'Stepping stones are numbered 0 through n. Start on stone 0 and reach stone n using jumps of exactly one or two positions. Some positive-numbered stones are broken and cannot be landed on. Count distinct sequences of jumps; crossing over a broken stone is allowed.',
        '0 ≤ n ≤ 200,000; broken indices are distinct and between 1 and n. Stone 0 is never broken.',
        'First line: n and broken-stone count. Second line: the broken indices, possibly empty.',
        'Print the number of routes modulo 1,000,000,007. When n is 0 there is one empty route.', bridge_tests)
    return result
