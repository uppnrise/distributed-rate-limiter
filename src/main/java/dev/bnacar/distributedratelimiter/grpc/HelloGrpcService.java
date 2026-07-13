package dev.bnacar.distributedratelimiter.grpc;

import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import dev.bnacar.distributedratelimiter.grpc.HelloServiceGrpc;
import dev.bnacar.distributedratelimiter.grpc.HelloRequest;
import dev.bnacar.distributedratelimiter.grpc.HelloReply;

@GrpcService
public class HelloGrpcService extends HelloServiceGrpc.HelloServiceImplBase {

    @Override
    public void sayHello(
            HelloRequest request,
            StreamObserver<HelloReply> responseObserver) {

        HelloReply reply = HelloReply.newBuilder()
                .setMessage("Hello " + request.getName() + " from Distributed Rate Limiter")
                .build();

        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }
}