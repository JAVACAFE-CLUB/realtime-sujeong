package ner;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * NER 서비스 정의
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.60.0)",
    comments = "Source: ner.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class NerServiceGrpc {

  private NerServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "ner.NerService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<ner.Ner.NerRequest,
      ner.Ner.NerResponse> getExtractEntitiesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ExtractEntities",
      requestType = ner.Ner.NerRequest.class,
      responseType = ner.Ner.NerResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<ner.Ner.NerRequest,
      ner.Ner.NerResponse> getExtractEntitiesMethod() {
    io.grpc.MethodDescriptor<ner.Ner.NerRequest, ner.Ner.NerResponse> getExtractEntitiesMethod;
    if ((getExtractEntitiesMethod = NerServiceGrpc.getExtractEntitiesMethod) == null) {
      synchronized (NerServiceGrpc.class) {
        if ((getExtractEntitiesMethod = NerServiceGrpc.getExtractEntitiesMethod) == null) {
          NerServiceGrpc.getExtractEntitiesMethod = getExtractEntitiesMethod =
              io.grpc.MethodDescriptor.<ner.Ner.NerRequest, ner.Ner.NerResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ExtractEntities"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  ner.Ner.NerRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  ner.Ner.NerResponse.getDefaultInstance()))
              .setSchemaDescriptor(new NerServiceMethodDescriptorSupplier("ExtractEntities"))
              .build();
        }
      }
    }
    return getExtractEntitiesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<ner.Ner.NerBatchRequest,
      ner.Ner.NerBatchResponse> getExtractEntitiesBatchMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ExtractEntitiesBatch",
      requestType = ner.Ner.NerBatchRequest.class,
      responseType = ner.Ner.NerBatchResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<ner.Ner.NerBatchRequest,
      ner.Ner.NerBatchResponse> getExtractEntitiesBatchMethod() {
    io.grpc.MethodDescriptor<ner.Ner.NerBatchRequest, ner.Ner.NerBatchResponse> getExtractEntitiesBatchMethod;
    if ((getExtractEntitiesBatchMethod = NerServiceGrpc.getExtractEntitiesBatchMethod) == null) {
      synchronized (NerServiceGrpc.class) {
        if ((getExtractEntitiesBatchMethod = NerServiceGrpc.getExtractEntitiesBatchMethod) == null) {
          NerServiceGrpc.getExtractEntitiesBatchMethod = getExtractEntitiesBatchMethod =
              io.grpc.MethodDescriptor.<ner.Ner.NerBatchRequest, ner.Ner.NerBatchResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ExtractEntitiesBatch"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  ner.Ner.NerBatchRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  ner.Ner.NerBatchResponse.getDefaultInstance()))
              .setSchemaDescriptor(new NerServiceMethodDescriptorSupplier("ExtractEntitiesBatch"))
              .build();
        }
      }
    }
    return getExtractEntitiesBatchMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static NerServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<NerServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<NerServiceStub>() {
        @java.lang.Override
        public NerServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new NerServiceStub(channel, callOptions);
        }
      };
    return NerServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static NerServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<NerServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<NerServiceBlockingStub>() {
        @java.lang.Override
        public NerServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new NerServiceBlockingStub(channel, callOptions);
        }
      };
    return NerServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static NerServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<NerServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<NerServiceFutureStub>() {
        @java.lang.Override
        public NerServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new NerServiceFutureStub(channel, callOptions);
        }
      };
    return NerServiceFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * NER 서비스 정의
   * </pre>
   */
  public interface AsyncService {

    /**
     * <pre>
     * 단일 텍스트 개체명 추출
     * </pre>
     */
    default void extractEntities(ner.Ner.NerRequest request,
        io.grpc.stub.StreamObserver<ner.Ner.NerResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getExtractEntitiesMethod(), responseObserver);
    }

    /**
     * <pre>
     * 배치 텍스트 개체명 추출 (대용량 처리)
     * </pre>
     */
    default void extractEntitiesBatch(ner.Ner.NerBatchRequest request,
        io.grpc.stub.StreamObserver<ner.Ner.NerBatchResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getExtractEntitiesBatchMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service NerService.
   * <pre>
   * NER 서비스 정의
   * </pre>
   */
  public static abstract class NerServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return NerServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service NerService.
   * <pre>
   * NER 서비스 정의
   * </pre>
   */
  public static final class NerServiceStub
      extends io.grpc.stub.AbstractAsyncStub<NerServiceStub> {
    private NerServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected NerServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new NerServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * 단일 텍스트 개체명 추출
     * </pre>
     */
    public void extractEntities(ner.Ner.NerRequest request,
        io.grpc.stub.StreamObserver<ner.Ner.NerResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getExtractEntitiesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * 배치 텍스트 개체명 추출 (대용량 처리)
     * </pre>
     */
    public void extractEntitiesBatch(ner.Ner.NerBatchRequest request,
        io.grpc.stub.StreamObserver<ner.Ner.NerBatchResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getExtractEntitiesBatchMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service NerService.
   * <pre>
   * NER 서비스 정의
   * </pre>
   */
  public static final class NerServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<NerServiceBlockingStub> {
    private NerServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected NerServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new NerServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * 단일 텍스트 개체명 추출
     * </pre>
     */
    public ner.Ner.NerResponse extractEntities(ner.Ner.NerRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getExtractEntitiesMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * 배치 텍스트 개체명 추출 (대용량 처리)
     * </pre>
     */
    public ner.Ner.NerBatchResponse extractEntitiesBatch(ner.Ner.NerBatchRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getExtractEntitiesBatchMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service NerService.
   * <pre>
   * NER 서비스 정의
   * </pre>
   */
  public static final class NerServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<NerServiceFutureStub> {
    private NerServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected NerServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new NerServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * 단일 텍스트 개체명 추출
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<ner.Ner.NerResponse> extractEntities(
        ner.Ner.NerRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getExtractEntitiesMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * 배치 텍스트 개체명 추출 (대용량 처리)
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<ner.Ner.NerBatchResponse> extractEntitiesBatch(
        ner.Ner.NerBatchRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getExtractEntitiesBatchMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_EXTRACT_ENTITIES = 0;
  private static final int METHODID_EXTRACT_ENTITIES_BATCH = 1;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_EXTRACT_ENTITIES:
          serviceImpl.extractEntities((ner.Ner.NerRequest) request,
              (io.grpc.stub.StreamObserver<ner.Ner.NerResponse>) responseObserver);
          break;
        case METHODID_EXTRACT_ENTITIES_BATCH:
          serviceImpl.extractEntitiesBatch((ner.Ner.NerBatchRequest) request,
              (io.grpc.stub.StreamObserver<ner.Ner.NerBatchResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getExtractEntitiesMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              ner.Ner.NerRequest,
              ner.Ner.NerResponse>(
                service, METHODID_EXTRACT_ENTITIES)))
        .addMethod(
          getExtractEntitiesBatchMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              ner.Ner.NerBatchRequest,
              ner.Ner.NerBatchResponse>(
                service, METHODID_EXTRACT_ENTITIES_BATCH)))
        .build();
  }

  private static abstract class NerServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    NerServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return ner.Ner.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("NerService");
    }
  }

  private static final class NerServiceFileDescriptorSupplier
      extends NerServiceBaseDescriptorSupplier {
    NerServiceFileDescriptorSupplier() {}
  }

  private static final class NerServiceMethodDescriptorSupplier
      extends NerServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    NerServiceMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (NerServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new NerServiceFileDescriptorSupplier())
              .addMethod(getExtractEntitiesMethod())
              .addMethod(getExtractEntitiesBatchMethod())
              .build();
        }
      }
    }
    return result;
  }
}
