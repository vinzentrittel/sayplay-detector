package de.vinzentrittel.detector.internal

import java.util.SortedMap
import kotlin.collections.iterator
import kotlin.collections.plus

typealias LineMap = SortedMap<Int, MutableList<Line>>
typealias LinePermutations = List<List<Line>>

val HASH_SETTING = HashSetting.ANGLE_HASH_PARAMETER
val OFFSET_90_DEGREES = hash(0.0, HASH_SETTING) - hash(-90.0, HASH_SETTING)

internal fun <T> getAllOrderedAlternatingTuples(
    firstList: List<T>, secondList: List<T>, tupleSize: Int
): List<List<T>> {
    /**
     * Creates all n-tuples from two lists. Each tuple consists of alternating elements from
     * [firstList] and [secondList]. The list's order persists. The tuple size 'n' is specified by
     * [tupleSize]. If [tupleSize] is unachievable the tuples will be as big as possible. All tuples
     * first elements are always from the longest list ([firstList] or [secondList]).
     *
     * @param T A generic type to permutate.
     * @param firstList A list to generate one half of all values for each tuple
     * @param secondList A list to generate the second half of all values for each tuple
     * @return a list of all ordered n-tuple permutations, were the order is specified by
     *         [firstList] and [secondList]
     */
    require(tupleSize > 0)
    val smallerList = if (firstList.size >= secondList.size) secondList else firstList
    val greaterList = if (firstList.size >= secondList.size) firstList else secondList

    val trueTupleSize: Int = (
        if (smallerList.size * 2 < tupleSize)
            smallerList.size * 2 + (
                if (smallerList.size == greaterList.size) 0 else 1
            )
        else tupleSize
    )

    return getAllOrderedAlternatingTuplesWithOrderedInput(
        greaterList, smallerList, listOf(), mutableListOf(), trueTupleSize
    )
}

private fun <T> getAllOrderedAlternatingTuplesWithOrderedInput(
    firstList: List<T>, secondList: List<T>, currentTuple: List<T>, result: MutableList<List<T>>, tupleSize: Int
): List<List<T>> {
    if (currentTuple.size == tupleSize) {
        if (!currentTuple.isEmpty())
            result.add(currentTuple)
        return result
    }
    if (currentTuple.size >= 3) {
        result.add(currentTuple)
    }
    val useFirstList = currentTuple.size % 2 == 0

    if (useFirstList) {
        for (i in 0 until firstList.size) {
            getAllOrderedAlternatingTuplesWithOrderedInput(
                firstList.subList(i + 1, firstList.size),
                secondList,
                currentTuple + listOf(firstList[i]),
                result,
                tupleSize
            )
        }
    } else {
        for (i in 0 until secondList.size) {
            getAllOrderedAlternatingTuplesWithOrderedInput(
                firstList,
                secondList.subList(i + 1, secondList.size),
                currentTuple + listOf(secondList[i]),
                result,
                tupleSize
            )
        }
    }

    return result
}

private fun getLinePermutations(lines: LineMap, result: LinePermutations?=null): LinePermutations {
    if (lines.isEmpty() || !potentiallyMorePerpendiculars(lines.firstKey())) {
        return result.orEmpty()
    }
    require(lines.containsKey(lines.firstKey() + OFFSET_90_DEGREES))

    return getLinePermutations(
        lines.tail(),
        result.orEmpty() + getAllOrderedAlternatingTuples(
            lines[lines.firstKey()]!!,
            lines[lines.firstKey() + OFFSET_90_DEGREES]!!,
            4
        )
    )
}

fun <K : Comparable<K>, V, M: SortedMap<K, V>> M.tail(): M {
    if (size < 2) {
        @Suppress("UNCHECKED_CAST")
        return sortedMapOf<K, V>() as M
    }

    @Suppress("UNCHECKED_CAST")
    return tailMap(keys.elementAt(1)) as M
}

fun getRectangles(lines: List<Line>): List<Polygon> {
    val perpendiculars: LineMap = filterConnectedPerpendiculars(lines)
    val permutations: LinePermutations = getLinePermutations(perpendiculars)
    val polygons: List<Polygon> = permutations.map { permutation ->
        Polygon.fromLines(
        permutation[0],
        permutation[1],
        permutation.getOrNull(2),
        permutation.getOrNull(3),
        HASH_SETTING.binSize * 2
    ) }
    val rectangles = polygons.filter { it.isRectangle(10.0) }.toList()
    return if (rectangles.isNotEmpty()) listOf(rectangles.maxBy { it.approximateArea() }) else emptyList()
}

private fun expandBins(lineMap: LineMap): LineMap {
    val result = sortedMapOf<Int, MutableList<Line>>()
    for (index in 0 until HASH_SETTING.binCount) {
        val nextIndex = (index + 1) % HASH_SETTING.binCount

        if (!(lineMap.containsKey(index) || lineMap.containsKey(nextIndex))) {
            continue
        }

        if (!lineMap.containsKey(nextIndex)) {
            result[index] = lineMap[index]!!
        } else if (!lineMap.containsKey(index)) {
            result[index] = lineMap[nextIndex]!!
        } else {
            result[index] = (lineMap[index]!! + lineMap[nextIndex]!!).toMutableList()
        }
    }
    return result
}

private fun filterConnectedPerpendiculars(lines: List<Line>, edgeDistanceThreshold: Double=400.0): LineMap {
    val perpendiculars: LineMap = filterPerpendiculars(lines)
    val closedPerpendiculars: LineMap = sortedMapOf()

    for ((angleHash, lines) in perpendiculars) {
        val currentPerpendiculars =  if (potentiallyMorePerpendiculars(angleHash))
            perpendiculars[angleHash + OFFSET_90_DEGREES]
        else
            perpendiculars[angleHash - OFFSET_90_DEGREES]

        closedPerpendiculars[angleHash] = lines.filter { line ->
            edgeDistanceThreshold > currentPerpendiculars!!.minOf { perpendicular ->
                line.squaredDistance(perpendicular)
            }
        }.toMutableList()
    }

    return closedPerpendiculars
}

private fun filterPerpendiculars(lines: List<Line>): LineMap {
    val groupedLines: LineMap = sortedMapOf()

    for(line in lines) {
        val hash = hash(line.angle, HASH_SETTING)
        groupedLines.getOrPut(hash) { mutableListOf() }.add(line)
    }

    val expandedGroupedLines: LineMap = expandBins(groupedLines)
    val mergedGroupedLines: LineMap = mergeLines(expandedGroupedLines)
    val perpendiculars: LineMap = sortedMapOf()

    if (mergedGroupedLines.size < 2) {
        mergedGroupedLines.clear()
        return sortedMapOf()
    }

    for(angleHash in mergedGroupedLines.keys) {
        if (!potentiallyMorePerpendiculars(angleHash)) {
            break
        } else if (
            !perpendiculars.containsKey(angleHash)
            && mergedGroupedLines.containsKey(angleHash + OFFSET_90_DEGREES)
        ) {
            perpendiculars[angleHash] = mergedGroupedLines[angleHash]!!
            perpendiculars[angleHash + OFFSET_90_DEGREES] = mergedGroupedLines[angleHash + OFFSET_90_DEGREES]!!
        }
    }

    return perpendiculars
}

private fun potentiallyMorePerpendiculars(hash: Int): Boolean {
    return hash < HASH_SETTING.binCount / 2
}

private fun mergeLines(lines: List<Line>): List<Line> {
    if (lines.isEmpty()) {
        return listOf()
    }
    val similarLines: List<Line> = lines.tail().filter { it.similar(lines.first()) }
    val head: List<Line> = similarLines
            .map { current -> lines.first() + current }
            .ifEmpty { lines.subList(0, 1) }
    return head + mergeLines(lines.tail().filter { similarLines.contains(it).not() })
}

private fun mergeLines(lineMap: LineMap): LineMap {
    val result = sortedMapOf<Int, MutableList<Line>>()
    for ((angleHash, lines) in lineMap) {
        result[angleHash] = mergeLines(lines).toMutableList()
    }
    return result
}

private fun <E, L: List<E>> L.tail(): L {
    @Suppress("UNCHECKED_CAST")
    return subList(1, size) as L
}