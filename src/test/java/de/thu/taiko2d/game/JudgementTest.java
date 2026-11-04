package de.thu.taiko2d.game;

import static org.junit.jupiter.api.Assertions.*;

import de.thu.taiko2d.model.Note;
import de.thu.taiko2d.model.NoteType;
import java.util.List;
import org.junit.jupiter.api.Test;

class JudgementEngineTest {
    private static Note don(int id, long t) { return new Note(id, t, NoteType.DON); }
    private static Note ka(int id, long t)  { return new Note(id, t, NoteType.KA); }

    @Test
    void exactHitIsPerfect() {
        JudgementEngine e = new JudgementEngine(List.of(don(1, 1000)));
        assertEquals(Judgement.PERFECT, e.hit(NoteType.DON, 1000));
        assertEquals(300, e.score());
    }

    @Test
    void windowBoundariesAreInclusive() {
        assertEquals(Judgement.PERFECT, new JudgementEngine(List.of(don(1, 1000))).hit(NoteType.DON, 1045));
        assertEquals(Judgement.GOOD,    new JudgementEngine(List.of(don(1, 1000))).hit(NoteType.DON, 1046));
        assertEquals(Judgement.GOOD,    new JudgementEngine(List.of(don(1, 1000))).hit(NoteType.DON, 900));
        assertNull(new JudgementEngine(List.of(don(1, 1000))).hit(NoteType.DON, 899));
    }

    @Test
    void wrongTypeDoesNotConsumeTheNote() {
        JudgementEngine e = new JudgementEngine(List.of(don(1, 1000)));
        assertNull(e.hit(NoteType.KA, 1000));
        assertEquals(Judgement.PERFECT, e.hit(NoteType.DON, 1000));   // still available
    }

    @Test
    void nearestMatchingNoteIsChosen() {
        JudgementEngine e = new JudgementEngine(List.of(don(1, 1000), don(2, 1080)));
        assertEquals(Judgement.PERFECT, e.hit(NoteType.DON, 1075));   // nearest is note 2 (5 ms away)
        assertEquals(Judgement.GOOD,    e.hit(NoteType.DON, 1075));   // then note 1 (75 ms away)
    }

    @Test
    void oneNoteIsConsumedByOnlyOnePress() {
        JudgementEngine e = new JudgementEngine(List.of(don(1, 1000)));
        assertNotNull(e.hit(NoteType.DON, 1000));
        assertNull(e.hit(NoteType.DON, 1000));
    }

    @Test
    void noteIsMissedOnceAfterTheWindow() {
        JudgementEngine e = new JudgementEngine(List.of(don(1, 1000)));
        assertEquals(0, e.update(1100));     // 100 ms late is still hittable
        assertEquals(1, e.update(1101));
        assertEquals(0, e.update(2000));     // not counted twice
        assertEquals(1, e.miss());
        assertNull(e.hit(NoteType.DON, 1101));
    }

    @Test
    void missResetsComboButKeepsMaxCombo() {
        JudgementEngine e = new JudgementEngine(List.of(don(1, 1000), ka(2, 1500), don(3, 2000)));
        e.hit(NoteType.DON, 1000);
        e.hit(NoteType.KA, 1500);
        e.update(2101);                      // note 3 missed
        assertEquals(0, e.combo());
        assertEquals(2, e.maxCombo());
    }

    @Test
    void accuracyUsesSpecFormula() {
        JudgementEngine e = new JudgementEngine(List.of(don(1, 1000), don(2, 2000)));
        e.hit(NoteType.DON, 1000);           // perfect
        e.hit(NoteType.DON, 2060);           // good
        assertEquals(100.0 * (300 + 100) / 600, e.accuracy(), 0.0001);
    }

    @Test
    void tieGoesToEarlierNote() {
        JudgementEngine e = new JudgementEngine(List.of(don(2, 1100), don(1, 900)));
        e.hit(NoteType.DON, 1000);           // both 100 ms away; the 900 ms note must go first
        assertEquals(1, e.update(1201));     // only the 1100 ms note is left to be missed
    }

    @Test
    void emptyChartIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new JudgementEngine(List.of()));
    }
}