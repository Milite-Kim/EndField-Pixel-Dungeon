/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;

/**
 * 아츠 부착 (ArtsAttachment)
 *
 * 아츠 속성 4종(열기/냉기/자연/전기)의 스택 버프.
 * 적은 한 번에 하나의 속성만 보유 가능.
 *
 * [동작 방식]
 * - 첫 부착          : 해당 속성 1스택 생성
 * - 동일 속성 재부착  : 아츠 폭발 → 해당 속성 피해 + 스택 +1 (최대 4)
 * - 다른 속성 부착    : 아츠 반응 → 기존 스택 전량 소모 후 반응 발동
 *
 * [아츠 반응 종류] (발동 속성 기준)
 *   HEAT     → 연소 (Combustion)  : 소모 스택 비례 열기 지속 피해
 *   CRYO     → 동결 (Frozen)      : 소모 스택 비례 이동 불가
 *   NATURE   → 부식 (ArtsCorrosion): 해당 적 공격 시 소량 회복
 *   ELECTRIC → 감전 (Electrified) : 받는 아츠 피해 증가
 *
 * 사용법:
 *   ArtsAttachment.apply(enemy, ArtsAttachment.ArtsType.HEAT, attacker);
 */
public class ArtsAttachment extends Buff {

    {
        type = buffType.NEGATIVE;
        announced = true;
    }

    // ─────────────────────────────────────────────
    // 아츠 속성 4종
    // ─────────────────────────────────────────────

    public enum ArtsType {
        HEAT,     // 열기
        CRYO,     // 냉기
        NATURE,   // 자연
        ELECTRIC  // 전기
    }

    public static final int MAX_STACKS = 4;

    private ArtsType currentType;
    private int stacks = 0;

    // ─────────────────────────────────────────────
    // 핵심 적용 메서드 (외부에서 이것만 호출)
    // ─────────────────────────────────────────────

    /**
     * 아츠 부착을 적에게 적용한다.
     *
     * @param enemy    대상 적
     * @param type     부착할 아츠 속성
     * @param attacker 아츠를 부착하는 공격자 (폭발 피해 계산 기준)
     */
    public static void apply(Char enemy, ArtsType type, Char attacker) {
        ArtsAttachment existing = enemy.buff(ArtsAttachment.class);

        if (existing == null) {
            // 첫 부착: 1스택 생성
            ArtsAttachment buff = Buff.affect(enemy, ArtsAttachment.class);
            buff.currentType = type;
            buff.stacks = 1;

        } else if (existing.currentType == type) {
            // 동일 속성 재부착: 아츠 폭발
            if (existing.stacks < MAX_STACKS) existing.stacks++;
            triggerArtsExplosion(enemy, type, existing.stacks, attacker);

        } else {
            // 다른 속성 부착: 아츠 반응
            int consumedStacks = existing.stacks;
            existing.detach();
            triggerArtsReaction(enemy, type, consumedStacks);
        }

        // 상태 변화 후 팀 오퍼레이터 연계기 조건 체크
        // 반응 결과(동결/부식/감전 등)도 포함하여 체크됨
        if (Dungeon.hero != null && enemy.isAlive()) {
            Dungeon.hero.checkChainTriggers(enemy);
        }
    }

    // ─────────────────────────────────────────────
    // 아츠 폭발 (동일 속성 재부착 시)
    // ─────────────────────────────────────────────

    /**
     * 아츠 폭발: 해당 속성 피해 + 스택 +1 (스택 유지).
     * 동일 속성 재부착 시 발동.
     *
     * 피해 = attacker.damageRoll() × stacks × EXPLOSION_MULT
     * TODO: 수치 확정 필요
     */
    private static final float EXPLOSION_MULT = 0.5f; // TODO: 수치 확정

    private static void triggerArtsExplosion(Char enemy, ArtsType type, int stacks, Char attacker) {
        DamageType damageType = toDamageType(type);
        int damage = Math.round(attacker.damageRoll() * stacks * EXPLOSION_MULT);
        enemy.damage(damage, attacker, damageType);
    }

    /** ArtsType → DamageType 변환 */
    private static DamageType toDamageType(ArtsType type) {
        switch (type) {
            case HEAT:     return DamageType.HEAT;
            case CRYO:     return DamageType.COLD;
            case NATURE:   return DamageType.NATURE;
            case ELECTRIC: return DamageType.ELECTRIC;
            default:       return DamageType.HEAT;
        }
    }

    // ─────────────────────────────────────────────
    // 아츠 반응 (다른 속성 부착 시)
    // ─────────────────────────────────────────────

    /**
     * 아츠 반응: 새로 부착된 속성에 따라 반응 발동.
     * 기존 스택은 이미 소모(detach)된 상태로 호출됨.
     *
     * @param newType        새로 부착된 속성 (반응 종류 결정)
     * @param consumedStacks 소모된 기존 스택 수
     */
    private static void triggerArtsReaction(Char enemy, ArtsType newType, int consumedStacks) {
        switch (newType) {
            case HEAT:
                // 연소: 소모 스택 비례 열기 지속 피해
                Combustion.apply(enemy, consumedStacks);
                break;

            case CRYO:
                // 동결: 소모 스택 비례 이동 불가
                // 1스택=1턴 / 2~3스택=2턴 / 4스택=3턴 / 고등급 면역
                Frozen.apply(enemy, consumedStacks);
                break;

            case NATURE:
                // 부식: 해당 적 공격 시 소량 회복
                ArtsCorrosion.apply(enemy, consumedStacks);
                break;

            case ELECTRIC:
                // 감전: 지속시간 동안 받는 아츠 피해 증가
                Electrified.apply(enemy, consumedStacks);
                break;
        }
    }

    // ─────────────────────────────────────────────
    // 조회
    // ─────────────────────────────────────────────

    public ArtsType currentType() { return currentType; }
    public int stacks()           { return stacks; }

    // ─────────────────────────────────────────────
    // 버프 유지 (턴마다 자동 소모 없음)
    // ─────────────────────────────────────────────

    @Override
    public boolean act() {
        spend(TICK);
        return true;
    }

    @Override
    public int icon() {
        return BuffIndicator.NONE; // TODO: 속성별 전용 아이콘
    }

    @Override
    public String iconTextDisplay() {
        return Integer.toString(stacks);
    }

    // ─────────────────────────────────────────────
    // 저장/불러오기
    // ─────────────────────────────────────────────

    private static final String TYPE   = "artsType";
    private static final String STACKS = "stacks";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(TYPE,   currentType);
        bundle.put(STACKS, stacks);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        currentType = bundle.getEnum(TYPE, ArtsType.class);
        stacks      = bundle.getInt(STACKS);
    }
}
