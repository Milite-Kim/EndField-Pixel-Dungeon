/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.shatteredpixel.shatteredpixeldungeon.operators;

import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;


/**
 * 오퍼레이터 기본 추상 클래스.
 * 메인 오퍼레이터와 팀 오퍼레이터 모두 이 클래스를 상속받습니다.
 */
public abstract class Operator implements Bundlable {

    // 직군
    public enum OperatorClass {
        STRIKER,    // 스트라이커 - 까다로운 조건의 강력한 연계기
        GUARD,      // 가드       - 방어불능 스택 축적 특화
        CASTER,     // 캐스터     - 아츠 부착 부여 특화
        SUPPORTER,  // 서포터     - 아군 강화 + 적 약화
        DEFENDER,   // 디펜더     - 아군 보호/패링
        VANGUARD    // 뱅가드     - 짧은 배틀스킬 쿨타임, 궁극기 충전 특화
    }

    // 무기 유형
    public enum WeaponType {
        ONE_HANDED_SWORD,   // 한손검  - 균형잡힌 데미지, 패널티 없음
        TWO_HANDED_SWORD,   // 양손검  - 최소~최대 격차 최대, 공격 시 2턴 소모
        POLEARM,            // 장병기  - 사거리 +1칸, 근접 시 데미지 감소
        HANDGUN,            // 권총    - 사거리 최대, 거리 멀수록 데미지↓
        ARTS_UNIT           // 아츠유닛 - 항상 명중, 배틀스킬 시 충전 +1
    }

    // 속성
    public enum Attribute {
        PHYSICAL,   // 물리
        FIRE,       // 열기
        COLD,       // 냉기
        NATURE,     // 자연
        ELECTRIC    // 전기
    }

    // 오퍼레이터 이름 (표시용)
    public abstract String name();

    // 직군
    public abstract OperatorClass operatorClass();

    // 무기 유형
    public abstract WeaponType weaponType();

    // 속성
    public abstract Attribute attribute();

    // 배틀스킬 (메인 오퍼레이터로 운용 시 사용 가능)
    public abstract BattleSkill battleSkill();

    // 궁극기 (메인 오퍼레이터로 운용 시 사용 가능)
    public abstract Ultimate ultimate();

    /**
     * 기본 공격 콤보 배율 배열.
     * 배열 길이 = 콤보 단계 수, 마지막 원소 = 강력한 일격.
     *
     * 오퍼레이터별로 오버라이드해 고유한 콤보 리듬을 정의.
     * ex) [0.8f, 0.8f, 1.3f]  →  약 → 약 → 강력한 일격
     *     [1.0f, 1.4f]        →  중 → 강력한 일격 (양손검 2단계)
     *
     * TODO: 각 오퍼레이터 파일에서 개별 수치 확정 필요
     */
    public float[] comboMultipliers() {
        switch (weaponType()) {
            case TWO_HANDED_SWORD:
                // 2단계: 중 → 강력한 일격
                return new float[]{ 1.0f, 1.4f };
            case POLEARM:
                // 3단계: 약 → 중 → 강력한 일격
                return new float[]{ 0.8f, 1.0f, 1.3f };
            case HANDGUN:
                // 3단계: 약 → 약 → 강력한 일격
                return new float[]{ 0.8f, 0.9f, 1.2f };
            case ARTS_UNIT:
                // 3단계: 약 → 약 → 강력한 일격 (자체 데미지 낮음)
                return new float[]{ 0.7f, 0.8f, 1.1f };
            case ONE_HANDED_SWORD:
            default:
                // 3단계: 약 → 약 → 강력한 일격
                return new float[]{ 0.8f, 0.9f, 1.2f };
        }
    }

    // 저장/불러오기 (현재는 비어있음, 서브클래스에서 필요 시 확장)
    @Override
    public void storeInBundle(Bundle bundle) {
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
    }
}
