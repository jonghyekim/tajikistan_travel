package egovframework.example.chatbot.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatbotLexiconTest {

    @Test
    void normalizesCommonRegionTypos() {
        assertThat(ChatbotLexicon.regionCode("두산베")).isEqualTo("DUSHANBE");
        assertThat(ChatbotLexicon.regionCode("dushnabe")).isEqualTo("DUSHANBE");
        assertThat(ChatbotLexicon.regionCode("душанбэ")).isEqualTo("DUSHANBE");
        assertThat(ChatbotLexicon.regionCode("душанде")).isEqualTo("DUSHANBE");
    }

    @Test
    void mapsInformalAndMisspelledCategories() {
        assertThat(ChatbotLexicon.categoryCode("밥집")).isEqualTo("DINING");
        assertThat(ChatbotLexicon.categoryCode("마싯는곳")).isEqualTo("DINING");
        assertThat(ChatbotLexicon.categoryCode("restarant")).isEqualTo("DINING");
        assertThat(ChatbotLexicon.categoryCode("рестаран")).isEqualTo("DINING");
        assertThat(ChatbotLexicon.categoryCode("мехмонхона")).isEqualTo("STAY");
        assertThat(ChatbotLexicon.categoryCode("отел")).isEqualTo("STAY");
        assertThat(ChatbotLexicon.categoryCode("поесть")).isEqualTo("DINING");
        assertThat(ChatbotLexicon.categoryCode("хурок")).isEqualTo("DINING");
        assertThat(ChatbotLexicon.categoryCode("бог")).isEqualTo("PARK");
    }

    @Test
    void normalizesEmergencyAliases() {
        assertThat(ChatbotLexicon.contactAlias("полицйи")).isEqualTo("police");
        assertThat(ChatbotLexicon.contactAlias("таъчили")).isEqualTo("ambulance");
        assertThat(ChatbotLexicon.contactAlias("сухтор")).isEqualTo("fire");
        assertThat(ChatbotLexicon.contactAlias("embasy")).isEqualTo("embassy");
    }
}
