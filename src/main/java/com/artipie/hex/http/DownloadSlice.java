/*
 * The MIT License (MIT) Copyright (c) 2020-2022 artipie.com
 * https://github.com/artipie/hexpm-adapter/blob/master/LICENSE.txt
 */

package com.artipie.hex.http;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.headers.Header;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsFull;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.StandardRs;
import com.jcabi.log.Logger;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import org.reactivestreams.Publisher;

/**
 * This slice returns content as bytes by Key from request path.
 */
public class DownloadSlice implements Slice {
    /**
     * Ctor.
     * @param storage Repository storage.
     */
    public DownloadSlice(final Storage storage) {
        this.storage = storage;
    }

    /**
     * Repository storage.
     */
    private final Storage storage;

    /**
     * Path to packages.
     */
    static final String PACKAGES = "packages";

    /**
     * Pattern for packages.
     */
    static final Pattern PACKAGES_PTRN = Pattern.compile(String.format("/%s/\\S+", PACKAGES));

    /**
     * Path to tarballs.
     */
    static final String TARBALLS = "tarballs";

    /**
     * Pattern for tarballs.
     */
    static final Pattern TARBALLS_PTRN = Pattern.compile(String.format("/%s/\\S+", TARBALLS));


    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        Key.From key = new Key.From(
            new RequestLineFrom(line).uri().getPath()
                .replaceFirst("/", "")
        );
        return new AsyncResponse(
            this.storage.exists(key).thenCompose(
                exist -> {
                    final CompletableFuture<Response> res;
                    if (exist) {
                        res = this.storage.value(key).thenApply(
                            value ->
                                new RsFull(
                                    RsStatus.OK,
                                    new Headers.From(
                                        new Header("Content-Type", "application/octet-stream")),
                                    value
                                )
                        );
                        Logger.info(this, "Get resource by ", key.string());
                    } else {
                        res = CompletableFuture.completedFuture(StandardRs.NOT_FOUND);
                    }
                    return res;
                }
            )
        );
    }
}