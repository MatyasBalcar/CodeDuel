package dev.codeduel.judge;

import java.io.ByteArrayOutputStream;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

/** Cancels the HTTP stream before an executor can force an unbounded response allocation. */
final class LimitedBodySubscriber implements HttpResponse.BodySubscriber<byte[]> {
  private final int limit;
  private final ByteArrayOutputStream output = new ByteArrayOutputStream();
  private final CompletableFuture<byte[]> body = new CompletableFuture<>();
  private Flow.Subscription subscription;

  LimitedBodySubscriber(int limit) {
    this.limit = limit;
  }

  @Override
  public CompletionStage<byte[]> getBody() {
    return body;
  }

  @Override
  public void onSubscribe(Flow.Subscription value) {
    subscription = value;
    value.request(1);
  }

  @Override
  public void onNext(List<ByteBuffer> buffers) {
    for (var buffer : buffers) {
      if (buffer.remaining() > limit - output.size()) {
        subscription.cancel();
        body.completeExceptionally(new JudgeUnavailableException());
        return;
      }
      byte[] bytes = new byte[buffer.remaining()];
      buffer.get(bytes);
      output.writeBytes(bytes);
    }
    subscription.request(1);
  }

  @Override
  public void onError(Throwable error) {
    body.completeExceptionally(error);
  }

  @Override
  public void onComplete() {
    body.complete(output.toByteArray());
  }
}
