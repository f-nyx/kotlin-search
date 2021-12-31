package be.rlab.nlp

import be.rlab.nlp.model.Language
import org.junit.jupiter.api.Test

class NormalizerTest {
    @Test
    fun normalize() {
        val normalizer = Normalizer.new(
            "Era fácil compartir cuando había comida suficiente, o apenas la suficiente, para seguir viviendo. " +
            "¿Pero cuando no la había? Entonces entraba en juego la fuerza; la fuerza se convertía en derecho; en " +
            "poder, y la herramienta del poder era la violencia, y su aliado más devoto, el ojo que no quiere ver",
            language = Language.SPANISH
        ).applyStemming()
            .removeDiacritics()
            .removePunctuation()
            .removeStopWords()
            .caseInsensitive()
        val result = normalizer.normalize()
        assert(result == "facil compart com suficient suficient segu viv entrab jueg fuerz " +
            "fuerz converti derech herramient violenci ali devot ojo"
        )
    }
}
