package sample.msg

import reactor.core.publisher.Mono

interface EventHandler {

    /**
     * Listen on a queue and process messages based on provided task definition
     *
     * @param concurrency - level of parallelization
     * @param task - describes how a message should be handled
     */
    fun handle(concurrency: Int, task: (message: String) -> Mono<Void>)
}