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

import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Crossbow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Glaive;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Longsword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Quarterstaff;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

import java.util.ArrayList;
import java.util.List;


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
        HEAT,       // 열기
        COLD,       // 냉기
        NATURE,     // 자연
        ELECTRIC    // 전기
    }

    // 오퍼레이터 이름 (표시용)
    public abstract String name();

    /**
     * 오퍼레이터 선택 화면에 표시할 일러스트 에셋 경로.
     * 일러스트가 없는 오퍼레이터는 null 반환 → placeholder 텍스트 표시.
     */
    public String illustration() {
        return null;
    }

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

    /**
     * 게임 시작 시 장착할 무기.
     * 무기 타입별 SPD placeholder 무기를 반환.
     * TODO: 오퍼레이터별 전용 무기 클래스 구현 후 각 서브클래스에서 오버라이드
     */
    public MeleeWeapon startingWeapon() {
        switch (weaponType()) {
            case TWO_HANDED_SWORD: return new Longsword();   // TODO: 전용 양손검으로 교체
            case POLEARM:          return new Glaive();      // TODO: 전용 장병기로 교체
            case HANDGUN:          return new Crossbow();    // TODO: 전용 권총으로 교체
            case ARTS_UNIT:        return new Quarterstaff();// TODO: 전용 아츠유닛으로 교체
            case ONE_HANDED_SWORD:
            default:               return new WornShortsword(); // TODO: 전용 한손검으로 교체
        }
    }

    /**
     * 이 오퍼레이터가 메인 오퍼레이터로 설정될 때 호출 (Hero.syncActiveOperator에서 호출).
     * 패시브 버프 부여 등 메인 전용 초기화를 수행할 때 오버라이드.
     * 기본값: no-op
     */
    public void onBecomeMain(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero hero) {}

    // ─────────────────────────────────────────────
    // 아츠유닛 충전 시스템
    // 배틀스킬 사용 시 충전 +1 (최대 3).
    // 충전 활성화는 별도 액션(CombatHUD 버튼)으로 수행.
    // ─────────────────────────────────────────────

    /** 현재 아츠유닛 충전량. */
    protected int artsCharges = 0;

    /** 아츠유닛 최대 충전량. */
    public static final int MAX_ARTS_CHARGES = 3;

    /**
     * 아츠유닛 배틀스킬 사용 후 충전 +1.
     * 배틀스킬 activate() 마지막에 호출할 것.
     * ARTS_UNIT 무기 타입이 아니면 무시.
     */
    public final void gainArtsCharge() {
        if (weaponType() == WeaponType.ARTS_UNIT && artsCharges < MAX_ARTS_CHARGES) {
            artsCharges++;
        }
    }

    /** 현재 충전량 반환. CombatHUD 버튼 활성화 여부 판단에 사용. */
    public int getArtsCharges() { return artsCharges; }

    /**
     * 아츠유닛 충전 활성화 효과.
     * CombatHUD의 충전 버튼 클릭 시 Hero.actArtsCharge()에서 호출.
     * 서브클래스에서 오버라이드해 오퍼레이터별 효과 구현.
     * 기본값: 충전 전량 소모 (효과 없음).
     */
    public void activateArtsCharge(Hero hero) {
        artsCharges = 0;
    }

    /**
     * 아츠유닛 충전 활성화 시 타겟 선택이 필요한지 여부.
     * true인 경우 CombatHUD 충전 버튼 클릭 시 타겟팅 모드로 진입.
     * 기본값: false (즉시 발동).
     */
    public boolean artsChargeNeedsTarget() { return false; }

    /**
     * 충전 활성화 액션 소비 턴 수.
     * @param charges 활성화 직전 충전량
     * @return 소비할 게임 턴 수 (기본 1턴)
     */
    public float artsChargeTurns(int charges) { return 1f; }

    /**
     * 타겟팅이 필요한 충전 활성화 (artsChargeNeedsTarget() == true 오퍼레이터용).
     * 기본 구현은 무타겟 버전으로 위임.
     */
    public void activateArtsCharge(Hero hero, Char target, int cell) {
        activateArtsCharge(hero);
    }

    /**
     * 게임 시작 시 소지할 오퍼레이터 고유 아이템 목록.
     * 기본값: 빈 목록 (공통 아이템은 Hero.initFromOperator()에서 지급).
     * TODO: 각 오퍼레이터 아이템 데이터 확정 후 서브클래스에서 오버라이드
     */
    public List<Item> startingItems() {
        return new ArrayList<>();
    }

    // 저장/불러오기
    private static final String ARTS_CHARGES_KEY = "artsCharges";

    @Override
    public void storeInBundle(Bundle bundle) {
        if (artsCharges != 0) {
            bundle.put(ARTS_CHARGES_KEY, artsCharges);
        }
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        artsCharges = bundle.getInt(ARTS_CHARGES_KEY);
    }
}
