package co.edu.uco.seguridad.pdp.authorization.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DecisionStateTests {

    @Test
    void allow_reports_itself_as_allowed() {
        assertThat(DecisionState.ALLOW.isAllow()).isTrue();
    }

    @Test
    void deny_does_not_report_itself_as_allowed() {
        assertThat(DecisionState.DENY.isAllow()).isFalse();
    }

    @Test
    void indeterminate_does_not_report_itself_as_allowed() {
        assertThat(DecisionState.INDETERMINATE.isAllow()).isFalse();
    }
}
