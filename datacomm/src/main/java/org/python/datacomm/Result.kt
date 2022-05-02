package org.python.datacomm

enum class ResultType {
    SUCCESS,
    FAILED
}

open class Result(val type: ResultType, val message: String? = null) {
    fun <T> toDataResult(): DataResult<T> {
        return this as DataResult<T>
    }

    fun <T> toData(): T = toDataResult<T>().data

    /** Pipes flow forward if this result is successful. Otherwise, halts pipe and returns current result. */
    inline infix fun pipe(func: (Result) -> Result): Result = when (type) {
        ResultType.SUCCESS -> func(this)
        else -> this
    }

    inline fun orElse(func: (Result) -> Result): Result = when (type) {
        ResultType.SUCCESS -> this
        else -> func(this)
    }

    /** Pipes data flow forward if this result is successful. Otherwise, halts pipe and returns current result. */
    inline fun <T> pipeData(func: (T) -> Result): Result = when (type) {
        ResultType.SUCCESS -> func(toDataResult<T>().data)
        else -> this
    }

    inline fun <T, U> pipeData(default: U? = null, func: (T) -> U): U? = when (type) {
        ResultType.SUCCESS -> func(toDataResult<T>().data)
        else -> default
    }


    companion object {
        val DEFAULT_SUCCESS = Result(type = ResultType.SUCCESS)

        inline fun <T> from(failMessage: String? = null, item: T, transform: (T) -> ResultType) = transform(item).let {
            when (it) {
                ResultType.FAILED -> Result(it, failMessage)
                else -> Result(it)
            }
        }

        inline fun fromBoolean(failMessage: String? = null, func: () -> Boolean) = if (func()) DEFAULT_SUCCESS else Result(ResultType.FAILED, failMessage)
        inline fun <T> fromObject(failMessage: String? = null, func: () -> T?): Result {
            val obj = func()
            return if (obj != null)
                DataResult<T>(type = ResultType.SUCCESS, data = obj, message = failMessage)
            else
                Result(ResultType.FAILED, failMessage)
        }
    }
}

class DataResult<T>(type: ResultType, val data: T, message: String? = null) : Result(type, message) {
    companion object {
        fun <T> from(data: T) = DataResult(ResultType.SUCCESS, data)
    }
}