/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.shatteredpixel.shatteredpixeldungeon.items.traits;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.EquipableItem;

/**
 * 기질 (Trait) — 오퍼레이터 무기를 대체하는 능력치·옵션 담당 장착 아이템.
 *
 * 오퍼레이터 고유 무기(룩 전용)와 별개로 장착되며,
 * 식각(강화) 수치와 요구 능력치를 통해 Hero의 피해 계산에 기여한다.
 *
 * 분류:
 *   CommonTrait  — 공용 기질 (1~5티어, 모든 오퍼레이터 장착 가능)
 *   SpecialTrait — 특수 기질 (5티어 전용, 지정 오퍼레이터 메인 시 고유 효과)
 *   ErodedTrait  — 침식된 기질 (저주받은 무기 대응, 상세 미확정)
 *
 * ─── 능력치 연동 ────────────────────────────────────────────────
 *   식각 수치   : enchantLevel() == level()  →  Hero.getEnchantmentLevel()
 *   요구 능력치 : requiredStat()             →  Hero.traitRequiredStat()
 *   공격 프록   : proc(attacker, defender, damage)  →  Hero.attackProc()
 */
public abstract class Trait extends EquipableItem {

    // ─────────────────────────────────────────────
    // 서브클래스 필수 구현
    // ─────────────────────────────────────────────

    /** 기질 등급 (1~5). */
    public abstract int tier();

    /**
     * 장착 요구 능력치 (STR).
     * STR < requiredStat() 시 피해량 감소 패널티가 Hero.skillDamageRoll()에 반영된다.
     */
    public abstract int requiredStat();

    // ─────────────────────────────────────────────
    // 식각 수치
    // ─────────────────────────────────────────────

    /**
     * 현재 식각 수치 (= Item.level()).
     * Hero.getEnchantmentLevel()이 이 값을 읽는다.
     */
    public final int enchantLevel() {
        return level();
    }

    // ─────────────────────────────────────────────
    // 장착 / 해제
    // ─────────────────────────────────────────────

    @Override
    public boolean doEquip(Hero hero) {
        // 기존 기질이 있으면 먼저 해제 (single=false: 시간 소모 별도로 계산하지 않음)
        Trait current = hero.belongings.trait;
        if (current != null && !current.doUnequip(hero, true, false)) {
            return false;
        }

        detach(hero.belongings.backpack);
        hero.belongings.trait = this;
        activate(hero);

        cursedKnown = true;
        if (cursed) {
            equipCursed(hero);
        }

        updateQuickslot();
        hero.spend(timeToEquip(hero));
        return true;
    }

    @Override
    public boolean doUnequip(Hero hero, boolean collect, boolean single) {
        if (!super.doUnequip(hero, collect, single)) return false;
        deactivate(hero);
        hero.belongings.trait = null;
        return true;
    }

    @Override
    public boolean isEquipped(Hero hero) {
        return hero.belongings.trait == this;
    }

    // ─────────────────────────────────────────────
    // 패시브 / 온히트 효과
    // ─────────────────────────────────────────────

    /**
     * 기질 장착(또는 게임 로드) 시 호출.
     * 지속 패시브 버프 부여 등의 초기화에 사용.
     * EquipableItem.activate(Char)를 Hero로 위임.
     */
    @Override
    public void activate(Char ch) {
        if (ch instanceof Hero) activate((Hero) ch);
    }

    /**
     * 기질 장착 시 호출 (Hero 타입 버전).
     * 서브클래스에서 오버라이드해 패시브 버프 부여 등을 구현.
     * 기본값: no-op
     */
    public void activate(Hero hero) {}

    /**
     * 기질 해제 시 호출.
     * activate(Hero)에서 부여한 버프를 제거할 때 오버라이드.
     * 기본값: no-op
     */
    public void deactivate(Hero hero) {}

    /**
     * 이 기질이 제공하는 ATK 보너스.
     * Hero.getATK()에서 참조한다.
     * CommonTrait / SpecialTrait는 티어에 따라 오버라이드.
     * ErodedTrait는 설계 확정 후 오버라이드 예정.
     */
    public int traitATK() {
        return 0;
    }

    /**
     * 공격 명중 시 proc 효과 (무기 인챈트 대응).
     * Hero.attackProc()에서 장착된 기질의 이 메서드를 호출한다.
     *
     * @param attacker 공격자
     * @param defender 피격자
     * @param damage   최종 피해량
     * @return 조정된 피해량 (기본값: 변경 없이 그대로 반환)
     */
    public int proc(Char attacker, Char defender, int damage) {
        return damage;
    }

    // ─────────────────────────────────────────────
    // 공용 수식어 패시브 훅 (Hero가 장착 기질을 참조)
    //   기본값은 모두 중립(효과 없음). 해당 수식어 기질만 오버라이드.
    //   자세한 내용: docs/엔픽던_기질시스템.md §6
    // ─────────────────────────────────────────────

    /** 기본 공격 피해 배율 ('강공'). Hero.finalAttackEfficiency()에서 곱연산. */
    public float basicAttackMultiplier() {
        return 1f;
    }

    /** 스킬 피해 배율 ('기예'). Hero.skillDamageRoll()에서 곱연산. */
    public float skillDamageMultiplier() {
        return 1f;
    }

    /** 명중률 배율 ('분쇄'). Hero.attackSkill()에서 곱연산. */
    public float accuracyMultiplier() {
        return 1f;
    }

    /** 공격 속도 배율 ('사기'). Hero.operatorAttackDelay()에서 delay를 이 값으로 나눔. */
    public float attackSpeedMultiplier() {
        return 1f;
    }

    /** 식각 효율 배율 ('효율'). Hero.getEnchantmentLevel()에서 곱연산. */
    public float enchantEfficiency() {
        return 1f;
    }

    /** 공격 사거리 보너스 ('방출'). TODO: Hero reach 시스템 구현 후 연동 (현재 스텁). */
    public int reachBonus() {
        return 0;
    }

    /**
     * 궁극기 사용 직후 호출 ('고통').
     * Ultimate.use()에서 발동 → 결전 준비형 효과(예: 일정 턴 공격력 증가)를 부여.
     * 기본값: no-op
     */
    public void onUltimateUsed(Hero hero) {}
}
