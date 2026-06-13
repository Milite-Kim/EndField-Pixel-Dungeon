/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.blobs.endfield;

/**
 * 냉기 폭풍 지대 (BlizzardField) — 탕탕 궁극기 전용 냉기 지대.
 *
 * 동작은 {@link ColdZone}과 동일(매 턴 냉기 피해, explosionDamage>0이면 종료 폭발)하나,
 * 탕탕 배틀스킬의 "궁극기 지속 중 시전 시 즉시 종료(폭발)" 트리거가
 * 스노우샤인의 ColdZone과 섞이지 않도록 **별도 Blob 식별자**로 분리한다.
 * (Blob은 클래스당 레벨에 1개 인스턴스이므로 클래스를 나누면 지대가 구분된다.)
 */
public class BlizzardField extends ColdZone {
}
