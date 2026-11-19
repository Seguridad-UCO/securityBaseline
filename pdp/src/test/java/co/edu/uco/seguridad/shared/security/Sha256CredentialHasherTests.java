package co.edu.uco.seguridad.shared.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class Sha256CredentialHasherTests {

    private static final String SECRET = "kq8Zr1vXo3Yy0d7mCqf2l9Gg5bTn4Hs6Ue1Wa0Jp2Rk";

    private final Sha256CredentialHasher hasher = new Sha256CredentialHasher();

    @Test
    void a_new_hash_is_sha256_and_never_contains_the_secret() {
        String hash = hasher.hash(SECRET);

        assertThat(hash).startsWith("sha256:").hasSize("sha256:".length() + 64).doesNotContain(SECRET);
        assertThat(hasher.needsRehash(hash)).isFalse();
    }

    @Test
    void the_same_secret_matches_and_a_different_one_does_not() {
        String hash = hasher.hash(SECRET);

        assertThat(hasher.matches(SECRET, hash)).isTrue();
        assertThat(hasher.matches(SECRET + "x", hash)).isFalse();
    }

    @Test
    void a_legacy_bcrypt_hash_still_matches_and_is_flagged_for_rehash() {
        String legacy = new BCryptPasswordEncoder().encode(SECRET);

        assertThat(hasher.matches(SECRET, legacy)).isTrue();
        assertThat(hasher.matches("otro-secreto", legacy)).isFalse();
        assertThat(hasher.needsRehash(legacy)).isTrue();
    }

    @Test
    void a_malformed_or_missing_hash_never_matches() {
        assertThat(hasher.matches(SECRET, "sha256:no-es-hexadecimal")).isFalse();
        assertThat(hasher.matches(SECRET, null)).isFalse();
        assertThat(hasher.matches(null, hasher.hash(SECRET))).isFalse();
        assertThat(hasher.needsRehash(null)).isTrue();
    }
}
