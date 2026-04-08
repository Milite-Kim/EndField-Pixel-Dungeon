/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.operators.team;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ArtsAttachment;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invulnerability;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LowTempInjection;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MoltenFlame;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.operators.BattleSkill;
import com.shatteredpixel.shatteredpixeldungeon.operators.TeamOperator;
import com.shatteredpixel.shatteredpixeldungeon.operators.Ultimate;

/**
 * 라스트 라이트 (LastLight)
 *
 * 직군: 스트라이커
 * 무기: 양손검
 * 속성: 냉기
 *
 * [패시브]   강력한 일격 적중 시 대상 열기 부착 흡수 → 녹아내린 불꽃 +1스택 (최대 4스택, 4스택 시 흡수 불가)
 *             → Hero에 MoltenFlame 버프가 있어야 발동 (메인 오퍼레이터로 운용 시 onBecomeMain에서 부여)
 *             → onFinishingBlowLanded에서 MoltenFlame.tryAbsorbHeat() 호출로 처리
 * [배틀스킬] 메인에게 저온 주입 부여 (턴 소모 없음).
 *             다음 강력한 일격 적중 시 소모 → 추가 냉기 피해 + 냉기 부착
 * [연계기]   조건: 적 냉기 부착 3스택 이상
 *             효과: 냉기 부착 전량 소모 → 스택 비례 대량 냉기 피해 + 궁극기 충전 (내부 충전)
 * [궁극기]   3회 대량 냉기 피해 + 시전 중 모든 피해 면역
 *             ※ 외부 궁극기 충전 불가 — 자체 연계기로만 충전
 */
public class LastLight extends TeamOperator {

    // ─────────────────────────────────────────────
    // 피해/수치 상수 (TODO: 수치 확정)
    // ─────────────────────────────────────────────

    /** 연계기 소모 스택당 냉기 피해 배율. TODO: 수치 확정 */
    private static final float CHAIN_STACK_MULT = 0.7f;

    /** 궁극기 1타 냉기 피해 배율. TODO: 수치 확정 */
    private static final float ULT_HIT_MULT = 1.5f;

    // ─────────────────────────────────────────────
    // 오퍼레이터 기본 정보
    // ─────────────────────────────────────────────

    @Override public String name()               { return "라스트 라이트"; }
    @Override public OperatorClass operatorClass() { return OperatorClass.STRIKER; }
    @Override public WeaponType weaponType()     { return WeaponType.TWO_HANDED_SWORD; }
    @Override public Attribute attribute()       { return Attribute.COLD; }

    // ─────────────────────────────────────────────
    // 패시브 훅: 메인 오퍼레이터로 설정될 때 MoltenFlame 버프 부여
    // ─────────────────────────────────────────────

    /**
     * 라스트 라이트가 메인 오퍼레이터가 될 때 호출.
     * Hero에 MoltenFlame 버프를 부여하여 패시브(열기 흡수) 활성화.
     */
    @Override
    public void onBecomeMain(Hero hero) {
        Buff.affect(hero, MoltenFlame.class);
    }

    // ─────────────────────────────────────────────
    // 배틀스킬: 저온 주입
    // ─────────────────────────────────────────────

    @Override
    public BattleSkill battleSkill() {
        return new BattleSkill() {

            @Override public int baseCooldown() { return 4; } // TODO: 수치 확정
            @Override public String name()        { return "저온 주입"; }
            @Override public String description() {
                return "즉시 저온 주입 부여 (턴 소모 없음).\n" +
                       "다음 강력한 일격 적중 시 소모 → 냉기 피해(×" + LowTempInjection.COLD_MULT + ") + 냉기 부착";
            }

            @Override public boolean selfTarget() { return true; }
            @Override public float castTime()     { return 0f; } // 턴 소모 없음

            @Override
            protected void activate(Hero hero, Char target, int cell) {
                Buff.affect(hero, LowTempInjection.class);
            }
        };
    }

    // ─────────────────────────────────────────────
    // 연계기: 냉기 결빙탄
    // ─────────────────────────────────────────────

    @Override public int baseCooldown() { return 5; } // TODO: 수치 확정

    @Override public String chainName()        { return "냉기 결빙탄"; }
    @Override public String chainDescription() {
        return "조건: 적 냉기 부착 3스택 이상\n" +
               "효과: 냉기 부착 전량 소모 → 스택 비례 대량 냉기 피해 + 궁극기 충전";
    }

    /**
     * 연계기 조건: 적이 냉기(CRYO) 부착 3스택 이상 보유.
     */
    @Override
    public boolean chainCondition(Hero hero, Char target) {
        if (target == null || !target.isAlive()) return false;
        ArtsAttachment arts = target.buff(ArtsAttachment.class);
        return arts != null
                && arts.currentType() == ArtsAttachment.ArtsType.CRYO
                && arts.stacks() >= 3;
    }

    /**
     * 연계기 효과: 냉기 부착 전량 소모 → 스택 비례 냉기 피해 + 궁극기 내부 충전.
     * 라스트 라이트 궁극기는 외부 충전 불가 — addChargeInternal() 사용.
     */
    @Override
    public void activateChain(Hero hero, Char target) {
        if (target == null || !target.isAlive()) return;

        ArtsAttachment arts = target.buff(ArtsAttachment.class);
        int consumedStacks = (arts != null) ? arts.stacks() : 0;

        if (arts != null) arts.detach();

        // 스택 비례 냉기 피해
        int dmg = Math.round(hero.damageRoll() * CHAIN_STACK_MULT * Math.max(consumedStacks, 1));
        target.damage(dmg, hero, DamageType.COLD);

        // 궁극기 내부 충전 (외부 충전 불가 → addChargeInternal)
        if (hero.activeUltimate != null) {
            hero.activeUltimate.addChargeInternal(25); // TODO: 충전량 수치 확정
        }
    }

    // ─────────────────────────────────────────────
    // 궁극기: 절대영도
    // ─────────────────────────────────────────────

    @Override
    public Ultimate ultimate() {
        return new Ultimate() {

            @Override public int maxCharge() { return 120; } // TODO: 수치 확정
            @Override public String name()   { return "절대영도"; }
            @Override public String description() {
                return "3회 대량 냉기 피해(×" + ULT_HIT_MULT + "/회) + 시전 중 피해 면역.\n" +
                       "※ 외부 궁극기 충전 불가. 자체 연계기(냉기 결빙탄)로만 충전.";
            }

            /** 외부 충전 차단 — 자체 연계기로만 충전 */
            @Override public boolean canReceiveExternalCharge() { return false; }

            @Override
            protected void activate(Hero hero, Char target, int cell) {
                if (target == null || !target.isAlive()) return;

                // 시전 중 피해 면역
                Buff.affect(hero, Invulnerability.class, 3f);

                // 3회 냉기 피해
                for (int i = 0; i < 3; i++) {
                    if (!target.isAlive()) break;
                    int dmg = Math.round(hero.damageRoll() * ULT_HIT_MULT);
                    target.damage(dmg, hero, DamageType.COLD);
                }
            }
        };
    }
}
