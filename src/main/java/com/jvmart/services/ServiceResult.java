package com.jvmart.services;

public sealed interface ServiceResult<T>
    permits ServiceResult.Success, ServiceResult.Failure {

    record Success<T>(T value) implements ServiceResult<T> {}
    record Failure<T>(String message) implements ServiceResult<T> {}
}
