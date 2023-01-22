/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/hexpm-adapter/blob/master/LICENSE.txt
 */
package com.artipie.hex.http;

import com.artipie.asto.Storage;
import com.artipie.http.Slice;
import com.artipie.http.auth.Action;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.BasicAuthSlice;
import com.artipie.http.auth.Permission;
import com.artipie.http.auth.Permissions;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.StandardRs;
import com.artipie.http.rt.ByMethodsRule;
import com.artipie.http.rt.RtRule;
import com.artipie.http.rt.RtRulePath;
import com.artipie.http.rt.SliceRoute;
import com.artipie.http.slice.SliceSimple;
import java.util.Optional;

/**
 * Artipie {@link Slice} for HexPm repository HTTP API.
 *
 * @since 0.1
 */
public final class HexSlice extends Slice.Wrap {

    /**
     * Ctor with default parameters for free access.
     *
     * @param storage The storage for package.
     */
    public HexSlice(final Storage storage) {
        this(
            storage,
            Permissions.FREE,
            (login, pwd) -> Optional.of(new Authentication.User("anonymous")));
    }

    /**
     * Ctor.
     *
     * @param storage The storage for package.
     * @param perms Access permissions.
     * @param users Concrete identities.
     */
    public HexSlice(final Storage storage, final Permissions perms, final Authentication users) {
        super(new SliceRoute(
            new RtRulePath(
                new RtRule.All(
                    new ByMethodsRule(RqMethod.GET),
                    new RtRule.Any(
                        new RtRule.ByPath(DownloadSlice.PACKAGES_PTRN),
                        new RtRule.ByPath(DownloadSlice.TARBALLS_PTRN)
                    )
                ),
                new BasicAuthSlice(
                    new DownloadSlice(storage),
                    users,
                    new Permission.ByName(perms, Action.Standard.READ)
                )
            ),
            new RtRulePath(
                new RtRule.All(
                    new ByMethodsRule(RqMethod.GET),
                    new RtRule.ByPath(UserSlice.USERS)
                ),
                new BasicAuthSlice(
                    new UserSlice(),
                    users,
                    new Permission.ByName(perms, Action.Standard.READ)
                )
            ),
            new RtRulePath(
                new RtRule.All(
                    new ByMethodsRule(RqMethod.POST),
                    new RtRule.ByPath(UploadSlice.PUBLISH)
                ),
                new BasicAuthSlice(
                    new UploadSlice(storage),
                    users,
                    new Permission.ByName(perms, Action.Standard.WRITE)
                )
            ),
            new RtRulePath(
                new RtRule.All(
                    new ByMethodsRule(RqMethod.POST),
                    new RtRule.ByPath(DocsSlice.DOCS_PTRN)
                ),
                new BasicAuthSlice(
                    new DocsSlice(),
                    users,
                    new Permission.ByName(perms, Action.Standard.READ)
                )
            ),
            new RtRulePath(
                RtRule.FALLBACK, new SliceSimple(StandardRs.NOT_FOUND)
            )
        )
        );
    }
}
