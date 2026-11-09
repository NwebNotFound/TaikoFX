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
        // 2026-11-09 anti-spam rule: 101..150 ms early misses the note, beyond 150 ms is ignored.
        assertEquals(Judgement.MISS, new JudgementEngine(List.of(don(1, 1000))).hit(NoteType.DON, 899));
        assertEquals(Judgement.MISS, new JudgementEngine(List.of(don(1, 1000))).hit(NoteType.DON, 850));
        assertNull(new JudgementEngine(List.of(don(1, 1000))).hit(NoteType.DON, 849));
    }

    @Test
    void wrongTypeMissesTheNote() {   // was: wrongTypeDoesNotConsumeTheNote (before the anti-spam rule)
        JudgementEngine e = new JudgementEngine(List.of(don(1, 1000), don(2, 2000)));
        assertEquals(Judgement.MISS, e.hit(NoteType.KA, 1000));
        assertNull(e.hit(NoteType.DON, 1000));                       // note 1 is gone
        assertEquals(0, e.combo());
        assertEquals(1, e.miss());
        assertEquals(Judgement.PERFECT, e.hit(NoteType.DON, 2000));
    }

    @Test
    void pressFarFromAnyNoteIsIgnored() {
        JudgementEngine e = new JudgementEngine(List.of(don(1, 1000)));
        assertNull(e.hit(NoteType.KA, 500));
        assertEquals(0, e.miss());
        assertEquals(Judgement.PERFECT, e.hit(NoteType.DON, 1000));
    }

    @Test
    void spammingBothDrumsBreaksTheCombo() {
        JudgementEngine e = new JudgementEngine(List.of(don(1, 1000), ka(2, 1250), don(3, 1500)));
        for (long t = 900; t <= 1600; t += 50) {   // mash KA then DON every 50 ms
            e.hit(NoteType.KA, t);
            e.hit(NoteType.DON, t);
        }
        assertTrue(e.miss() > 0);
    }

    @Test
    void earliestMatchingNoteIsChosen() {   // was: nearestMatchingNoteIsChosen (changed 2026-11-09)
        JudgementEngine e = new JudgementEngine(List.of(don(1, 1000), don(2, 1080)));
        assertEquals(Judgement.GOOD,    e.hit(NoteType.DON, 1075));   // late on note 1, not note 2 early
        assertEquals(Judgement.PERFECT, e.hit(NoteType.DON, 1080));   // note 2 is still there
    }

    @Test
    void latePressInAStreamDoesNotCascade() {
        // 1/4 stream at 175 BPM (86 ms gaps), alternating colours, every press 60 ms late.
        List<Note> stream = new java.util.ArrayList<>();
        for (int i = 0; i < 8; i++) stream.add(i % 2 == 0 ? don(i, 1000 + i * 86L) : ka(i, 1000 + i * 86L));
        JudgementEngine e = new JudgementEngine(stream);
        for (int i = 0; i < 8; i++) {
            assertEquals(Judgement.GOOD, e.hit(i % 2 == 0 ? NoteType.DON : NoteType.KA, 1060 + i * 86L));
        }
        assertEquals(0, e.miss());
    }

    @Test
    void passedNoteCanBeSkippedButUpcomingNoteCannot() {
        JudgementEngine e = new JudgementEngine(List.of(don(1, 1000), ka(2, 1086), don(3, 1172)));
        assertEquals(Judgement.PERFECT, e.hit(NoteType.KA, 1086));    // note 1 already passed: skip it
        assertEquals(0, e.miss());
        assertEquals(Judgement.MISS, e.hit(NoteType.KA, 1150));       // note 3 is upcoming DON: blocked
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