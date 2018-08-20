package net

interface SizePredictor {
    fun sizeOf(data: Any): Int
}