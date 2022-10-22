package de.huebcraft.configlib.config

class MutableCollectionWrapper<T>(private val wrapped: MutableCollection<T>, private val top: ConfigObject) :
    MutableCollection<T> {
    override val size: Int
        get() = wrapped.size

    override fun clear() {
        wrapped.clear()
        top.schedulePersist()
    }

    override fun addAll(elements: Collection<T>): Boolean {
        val result = wrapped.addAll(elements)
        if (result) {
            top.schedulePersist()
        }
        return result
    }

    override fun add(element: T): Boolean {
        val result = wrapped.add(element)
        if (result) {
            top.schedulePersist()
        }
        return result
    }

    override fun isEmpty() = wrapped.isEmpty()

    override fun iterator(): MutableIterator<T> = MutableIteratorWrapper(wrapped.iterator(), top)

    override fun retainAll(elements: Collection<T>): Boolean {
        val result = wrapped.retainAll(elements.toSet())
        if (result) {
            top.schedulePersist()
        }
        return result
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        val result = wrapped.removeAll(elements.toSet())
        if (result) {
            top.schedulePersist()
        }
        return result
    }

    override fun remove(element: T): Boolean {
        val result = wrapped.remove(element)
        if (result) {
            top.schedulePersist()
        }
        return result
    }

    override fun containsAll(elements: Collection<T>) = wrapped.containsAll(elements)

    override fun contains(element: T) = wrapped.contains(element)
}