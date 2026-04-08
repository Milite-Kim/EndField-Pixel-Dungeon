/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.operators.team;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ArtsAttachment;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Electrified;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.operators.BattleSkill;
import com.shatteredpixel.shatteredpixeldungeon.operators.TeamOperator;
import com.shatteredpixel.shatteredpixeldungeon.operators.Ultimate;
import com.watabou.utils.Bundle;

/**
 * 아크라이트 (Arclight)
 *
 * 직군: 뱅가드
 * 무기: 한손검
 * 속성: 전기
 *
 * [배틀스킬] 2회 물리 피해. 감전 상태 적 → 감전 소모 → 추가 전기 피해 + 궁극기 충전
 *
 * [연계기]   조건: 대상이 감전 상태이거나, 배틀스킬로 감전이 방금 소모됐을 때
 *             효과: 물리 피해 + 궁극기 충전
 *             특수: 연계기 쿨타임이 자연 만료될 때마다 연계기 스택 +1 (최대 2).
 *                   스택 소비 방식으로 발동. 스택 0이면 발동 불가.
 *
 * [궁극기]   2회 전기 피해 + 전기 부착. 전기 부착 보유 적 → 소모 → 감전
 *
 * [스택 충전 메커니즘]
 *   - 내부 쿨타임 타이머(CHARGE_CYCLE_TURNS턴)가 만료될 때마다 charges +1 (최대 MAX_CHARGES)
 *   - 스택이 있으면 isReady() = true → 연계기 발동 가능
 *   - 연계기 발동 시 charges-- (타이머 유지)
 *   - reduceCooldownByHalf / forceCooldownReduction → 현재 사이클 가속
 */
public class Arclight extends TeamOperator {

    // ─────────────────────────────────────────────
    // 피해/수치 상수 (TODO: 수치 확정)
    // ─────────────────────────────────────────────

    /** 배틀스킬 1회당 물리 피해 배율 (2회 반복). TODO: 수치 확정 */
    private static final float SKILL_HIT_MULT = 0.8f;

    /** 배틀스킬 감전 소모 시 추가 전기 피해 배율. TODO: 수치 확정 */
    private static final float SKILL_ELEC_BONUS_MULT = 1.2f;

    /** 배틀스킬 감전 소모 시 궁극기 충전량. TODO: 수치 확정 */
    private static final int SKILL_ULT_CHARGE = 20;

    /** 연계기 물리 피해 배율. TODO: 수치 확정 */
    private static final float CHAIN_MULT = 1.0f;

    /** 연계기 궁극기 충전량. TODO: 수치 확정 */
    private static final int CHAIN_ULT_CHARGE = 25;

    /** 궁극기 1타 전기 피해 배율 (2회 반복). TODO: 수치 확정 */
    private static final float ULT_HIT_MULT = 1.0f;

    // ─────────────────────────────────────────────
    // 충전 스택 시스템
    // ─────────────────────────────────────────────

    /** 스택 1개 충전까지 걸리는 쿨타임 사이클(턴). TODO: 수치 확정 */
    private static final int CHARGE_CYCLE_TURNS = 10;

    /** 최대 충전 스택. */
    private static final int MAX_CHARGES = 2;

    /** 현재 충전 스택 (0~MAX_CHARGES). isReady() = charges > 0 */
    private int charges = 0;

    // 초기 쿨타임 사이클 시작: cooldown이 protected이므로 직접 설정
    {
        cooldown = CHARGE_CYCLE_TURNS;
    }

    /**
     * 감전 소모 직후 checkChainTriggers 도중 true.
     * 이 시점에는 Electrified가 이미 detach됐으므로 buff 체크 불가 → context flag 사용.
     */
    private transient boolean electricConsumedContext = false;

    // ─────────────────────────────────────────────
    // 충전 사이클 오버라이드
    // ─────────────────────────────────────────────

    /**
     * 매 턴 쿨타임 차감. 0 도달 시 스택 +1(최대) 후 사이클 재시작.
     */
    @Override
    public void reduceCooldown() {
        if (cooldown <= 0) return; // 이미 0이면 무시 (외부 강제 감소 후 상태)
        cooldown--;
        if (cooldown == 0) {
            completeCycle();
        }
    }

    /**
     * 쿨타임을 즉시 절반으로 줄인다. 0 도달 시 사이클 완료.
     * Akekuri 궁극기 "소대 집합" 효과.
     */
    @Override
    public void reduceCooldownByHalf() {
        cooldown = cooldown / 2;
        if (cooldown == 0) completeCycle();
    }

    /**
     * 쿨타임을 지정 값만큼 줄인다 (최솟값 0). 0 도달 시 사이클 완료.
     * Akekuri 궁극기 지속 효과.
     */
    @Override
    public void forceCooldownReduction(int amount) {
        cooldown = Math.max(0, cooldown - amount);
        if (cooldown == 0) completeCycle();
    }

    /** 사이클 만료 처리: 스택 +1 후 재시작. */
    private void completeCycle() {
        if (charges < MAX_CHARGES) charges++;
        cooldown = CHARGE_CYCLE_TURNS; // 사이클 재시작
    }

    /** 스택이 있으면 연계기 발동 가능. */
    @Override
    public boolean isReady() {
        return charges > 0;
    }

    /** 연계기 발동 시 스택 소비 (쿨타임 타이머 유지). */
    @Override
    public void resetCooldown() {
        if (charges > 0) charges--;
        // cooldown 타이머는 계속 진행
    }

    @Override
    public int baseCooldown() { return CHARGE_CYCLE_TURNS; }

    // ─────────────────────────────────────────────
    // 오퍼레이터 기본 정보
    // ─────────────────────────────────────────────

    @Override public String name()               { return "아크라이트"; }
    @Override public OperatorClass operatorClass() { return OperatorClass.VANGUARD; }
    @Override public WeaponType weaponType()     { return WeaponType.ONE_HANDED_SWORD; }
    @Override public Attribute attribute()       { return Attribute.ELECTRIC; }

    // ─────────────────────────────────────────────
    // 배틀스킬: 이중 타격
    // ─────────────────────────────────────────────

    @Override
    public BattleSkill battleSkill() {
        return new BattleSkill() {

            @Override public int baseCooldown()  { return 3; } // TODO: 수치 확정
            @Override public String name()       { return "이중 타격"; }
            @Override public String description() {
                return "2회 물리 피해(×" + SKILL_HIT_MULT + "/회).\n" +
                       "감전 상태 적 → 감전 소모 → 추가 전기 피해(×" + SKILL_ELEC_BONUS_MULT + ") + 궁극기 충전";
            }

            @Override
            protected void activate(Hero hero, Char target, int cell) {
                if (target == null || !target.isAlive()) return;

                // 2회 물리 피해
                for (int i = 0; i < 2; i++) {
                    if (!target.isAlive()) break;
                    int dmg = Math.round(hero.damageRoll() * SKILL_HIT_MULT);
                    target.damage(dmg, hero, DamageType.PHYSICAL);
                }

                if (!target.isAlive()) return;

                // 감전 소모 → 추가 전기 피해 + 궁극기 충전
                Electrified electrified = target.buff(Electrified.class);
                if (electrified != null) {
                    electrified.detach();

                    int bonusDmg = Math.round(hero.damageRoll() * SKILL_ELEC_BONUS_MULT);
                    target.damage(bonusDmg, hero, DamageType.ELECTRIC);

                    if (hero.activeUltimate != null) {
                        hero.activeUltimate.addCharge(SKILL_ULT_CHARGE);
                    }

                    // 감전 소모 이벤트: 아크라이트 연계기 트리거
                    electricConsumedContext = true;
                    hero.checkChainTriggers(target);
                    electricConsumedContext = false;
                }
            }
        };
    }

    // ─────────────────────────────────────────────
    // 연계기: 전격 추격
    // ─────────────────────────────────────────────

    @Override public String chainName()        { return "전격 추격"; }
    @Override public String chainDescription() {
        return "조건: 감전 상태 or 감전 소모 시 (최대 " + MAX_CHARGES + "스택 사전 충전)\n" +
               "효과: 물리 피해(×" + CHAIN_MULT + ") + 궁극기 충전";
    }

    /**
     * 연계기 조건: 대상이 감전 상태이거나, 배틀스킬로 감전을 방금 소모했을 때.
     */
    @Override
    public boolean chainCondition(Hero hero, Char target) {
        if (target == null || !target.isAlive()) return false;
        return target.buff(Electrified.class) != null || electricConsumedContext;
    }

    /** 연계기 효과: 물리 피해 + 궁극기 충전 */
    @Override
    public void activateChain(Hero hero, Char target) {
        if (target == null || !target.isAlive()) return;

        int dmg = Math.round(hero.damageRoll() * CHAIN_MULT);
        target.damage(dmg, hero, DamageType.PHYSICAL);

        if (hero.activeUltimate != null) {
            hero.activeUltimate.addCharge(CHAIN_ULT_CHARGE);
        }
    }

    // ─────────────────────────────────────────────
    // 궁극기: 초고압 방전
    // ─────────────────────────────────────────────

    @Override
    public Ultimate ultimate() {
        return new Ultimate() {

            @Override public int maxCharge() { return 100; } // TODO: 수치 확정
            @Override public String name()   { return "초고압 방전"; }
            @Override public String description() {
                return "2회 전기 피해(×" + ULT_HIT_MULT + "/회) + 전기 부착.\n" +
                       "전기 부착 보유 적 → 소모 → 감전";
            }

            @Override
            protected void activate(Hero hero, Char target, int cell) {
                if (target == null || !target.isAlive()) return;

                // 전기 부착 사전 체크 (소모 여부 결정)
                ArtsAttachment existing = target.buff(ArtsAttachment.class);
                boolean hasElecAttach = existing != null
                        && existing.currentType() == ArtsAttachment.ArtsType.ELECTRIC;

                // 2회 전기 피해
                for (int i = 0; i < 2; i++) {
                    if (!target.isAlive()) break;
                    int dmg = Math.round(hero.damageRoll() * ULT_HIT_MULT);
                    target.damage(dmg, hero, DamageType.ELECTRIC);
                }

                if (!target.isAlive()) return;

                // 전기 부착 보유 시 소모 → 감전 (ArtsAttachment.apply로 이미 있는 ELECTRIC → 감전 반응)
                if (hasElecAttach) {
                    // 다른 속성으로 반응 유발: 이미 전기 부착이면 열기로 반응 → 연소 생성이 부자연스러움
                    // 대신 직접 감전 적용
                    existing = target.buff(ArtsAttachment.class);
                    if (existing != null) existing.detach();
                    Buff.affect(target, Electrified.class);
                    Electrified.apply(target, 1);
                } else {
                    // 전기 부착 없으면 신규 부착
                    ArtsAttachment.apply(target, ArtsAttachment.ArtsType.ELECTRIC, hero);
                }
            }
        };
    }

    // ─────────────────────────────────────────────
    // 저장/불러오기 (charges 추가)
    // ─────────────────────────────────────────────

    private static final String CHARGES = "charges";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(CHARGES, charges);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        charges = bundle.getInt(CHARGES);
    }
}
