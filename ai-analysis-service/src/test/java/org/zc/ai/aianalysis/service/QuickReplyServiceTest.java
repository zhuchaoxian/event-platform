package org.zc.ai.aianalysis.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QuickReplyServiceTest {

    private final QuickReplyService service = new QuickReplyService();

    @Test
    void shouldMatchHelpAndHealthRequests() {
        assertThat(service.supports("help?")).isTrue();
        assertThat(service.supports("你能做什么？")).isTrue();
        assertThat(service.supports("ping")).isTrue();
    }
}
