/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.actors;

/**
 * 피해 속성 타입.
 *
 * 물리 피해와 아츠 피해를 구분하며,
 * 아츠 피해는 속성별(열기/냉기/자연/전기)로 세분화된다.
 *
 * 기본 공격, 배틀스킬, 궁극기, 연계기 등에서 DamageType을 지정해
 * 감전(Electrified) 배율, 속성 저항/취약 등이 올바르게 적용되도록 한다.
 */
public enum DamageType {

    /** 물리 피해 */
    PHYSICAL,

    /** 아츠 피해 - 열기 */
    HEAT,

    /** 아츠 피해 - 냉기 */
    COLD,

    /** 아츠 피해 - 자연 */
    NATURE,

    /** 아츠 피해 - 전기 */
    ELECTRIC;

    /** 아츠 속성(열기/냉기/자연/전기) 여부 */
    public boolean isArts() {
        return this != PHYSICAL;
    }
}
