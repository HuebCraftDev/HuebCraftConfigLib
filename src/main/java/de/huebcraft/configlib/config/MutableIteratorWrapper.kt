package de.huebcraft.configlib.config

@PublishedApi
internal class MutableIteratorWrapper<T>(private val wrapped: MutableIterator<T>, private val top: ConfigObject) :
    MutableIterator<T> {
    override fun hasNext() = wrapped.hasNext()

    override fun next() = wrapped.next()

    override fun remove() {
        wrapped.remove()
        top.schedulePersist()
    }
}
