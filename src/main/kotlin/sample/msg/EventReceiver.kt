package com.nike.content.notary.message

import reactor.core.publisher.Flux

interface EventReceiver<T> {

    /**
     * Listen on a queue and get a stream of data
     */
    fun listen(): Flux<T>
}