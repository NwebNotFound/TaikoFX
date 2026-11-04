package de.thu.taiko2d.model;

public record Note(int id, long timeMs, NoteType noteType) {

    public static double noteX(double hitX, long scheduleMs, double songMs, double pixelsPerMs){
        return hitX + (scheduleMs-songMs) * pixelsPerMs;
    }
}
