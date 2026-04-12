/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.DamageType;

/**
 * 오리지늄 아츠 결정 (Originium Arts Crystal)
 *
 * 관리자(Endministrator)의 연계기로 적에게 부여되는 버프.
 * 최대 1스택 — 버프 존재 자체가 "결정 보유" 상태를 의미한다.
 *
 * 소모 조건: 적이 물리 이상(DefenselessStack)을 받을 때 소모 → 물리 피해
 * 관리자 궁극기에서도 전량 소모 가능.
 *
 * TODO: 피해 수치 확정
 */
public class OriginiumCrystal extends Buff {

    /** 결정 소모 1회당 피해 배율. TODO: 수치 확정 */
    private static final float CONSUME_DMG_MULT = 0.5f;

    // ─────────────────────────────────────────────
    // 적용 (정적 진입점)
    // ─────────────────────────────────────────────

    /** 적에게 오리지늄 아츠 결정을 부여한다. 이미 있으면 무시(하드캡 1). */
    public static void apply(Char enemy, int addStacks) {
        Buff.affect(enemy, OriginiumCrystal.class);
        // 최대 1스택 — 버프 존재 여부만 관리하므로 addStacks 무시
    }

    // ─────────────────────────────────────────────
    // 소모
    // ─────────────────────────────────────────────

    /**
     * 물리 이상(DefenselessStack) 적중 시 결정 소모 → 물리 피해 + 버프 제거.
     *
     * @param enemy    결정 보유 적
     * @param attacker 물리 이상을 가한 공격자 (피해 계산 기준)
     */
    public void triggerConsumption(Char enemy, Char attacker) {
        int damage = Math.round(attacker.damageRoll() * CONSUME_DMG_MULT);
        enemy.damage(damage, attacker, DamageType.PHYSICAL);
        detach();
    }

    /**
     * 전량 소모 (궁극기 발동 시 호출).
     *
     * @return 소모된 스택 수 — 항상 1 (추가 피해 계산에 사용)
     */
    public int consumeAll() {
        detach();
        return 1;
    }

    // storeInBundle / restoreFromBundle: 필드가 없으므로 Buff 기본 구현으로 충분
}
