/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.operators.team;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ArtsCorrosion;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Combustion;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LevatineUltimateBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MoltenFlame;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.operators.BattleSkill;
import com.shatteredpixel.shatteredpixeldungeon.operators.TeamOperator;
import com.shatteredpixel.shatteredpixeldungeon.operators.Ultimate;

/**
 * 레바테인 (Levatine)
 *
 * 직군: 스트라이커
 * 무기: 한손검
 * 속성: 열기
 *
 * ※ 메인 오퍼레이터 권장 캐릭터 — 패시브(MoltenFlame)는 메인일 때만 활성화.
 *
 * [패시브] 강력한 일격 시 대상 열기 부착 전량 소모 → 녹아내린 불꽃(MoltenFlame) +1스택 (최대 4)
 *           → Hero.onFinishingBlowLanded() + MoltenFlame.tryAbsorbHeat() 로 구현.
 *           → onBecomeMain()에서 MoltenFlame 버프를 Hero에게 최초 부여.
 *
 * [배틀스킬] 녹아내린 불꽃 조건 분기:
 *             4스택 미만: 열기 피해(×SKILL_MULT) + 불꽃 +1스택
 *             4스택 (or 궁극기 강화 준비 중): 전량 소모 →
 *               대량 열기 피해(×SKILL_ENHANCED_MULT) + 강제 연소(4스택) + 궁극기 충전
 *             궁극기 활성 중: 전체 배율 × LevatineUltimateBuff.DMG_MULT 추가 적용
 *
 * [연계기] 조건: 연소(Combustion) or 부식(ArtsCorrosion) 상태 시
 *           효과: 열기 피해 + 녹아내린 불꽃 +1스택 + 궁극기 충전
 *
 * [궁극기] 용광로의 불꽃 — LevatineUltimateBuff 부여:
 *           - 데미지 배율 증가 (DMG_MULT)
 *           - 매 FLAME_INTERVAL턴마다 불꽃 자동 +1스택
 *           - 배틀스킬 1회 강화 준비
 *           TODO: 사거리 +1칸 — Hero 공격 범위 시스템 구현 후 추가
 */
public class Levatine extends TeamOperator {

    // ─────────────────────────────────────────────
    // 피해/수치 상수 (TODO: 수치 확정)
    // ─────────────────────────────────────────────

    /** 배틀스킬 기본 열기 피해 배율. TODO: 수치 확정 */
    private static final float SKILL_MULT          = 0.8f;

    /** 배틀스킬 강화 모드 열기 피해 배율 (4스택 소모 시). TODO: 수치 확정 */
    private static final float SKILL_ENHANCED_MULT = 2.5f;

    /** 연계기 열기 피해 배율. TODO: 수치 확정 */
    private static final float CHAIN_MULT          = 1.0f;

    /** 배틀스킬 강화 시 궁극기 충전량. TODO: 수치 확정 */
    private static final int SKILL_ULT_CHARGE  = 20;

    /** 연계기 궁극기 충전량. TODO: 수치 확정 */
    private static final int CHAIN_ULT_CHARGE  = 15;

    // ─────────────────────────────────────────────
    // 오퍼레이터 기본 정보
    // ─────────────────────────────────────────────

    @Override public String name()               { return "레바테인"; }
    @Override public OperatorClass operatorClass() { return OperatorClass.STRIKER; }
    @Override public WeaponType weaponType()     { return WeaponType.ONE_HANDED_SWORD; }
    @Override public Attribute attribute()       { return Attribute.FIRE; }

    // ─────────────────────────────────────────────
    // 패시브: 메인 오퍼레이터 전환 시 MoltenFlame 버프 부여
    // ─────────────────────────────────────────────

    /**
     * 레바테인이 메인 오퍼레이터로 설정될 때 호출.
     * Hero에게 MoltenFlame 버프를 부여하여 패시브 활성화.
     * (이후 Hero.onFinishingBlowLanded → MoltenFlame.tryAbsorbHeat()로 스택 관리)
     */
    @Override
    public void onBecomeMain(Hero hero) {
        Buff.affect(hero, MoltenFlame.class);
    }

    // ─────────────────────────────────────────────
    // 배틀스킬: 녹아내린 불꽃 충전/폭발
    // ─────────────────────────────────────────────

    @Override
    public BattleSkill battleSkill() {
        return new BattleSkill() {

            @Override public int baseCooldown()  { return 4; } // TODO: 수치 확정
            @Override public String name()       { return "녹아내린 불꽃"; }
            @Override public String description() {
                return "4스택 미만: 열기 피해(×" + SKILL_MULT + ") + 불꽃 +1스택\n" +
                       "4스택 (or 궁극기 강화): 소모 → 대량 열기 피해(×" + SKILL_ENHANCED_MULT + ") + 강제 연소 + 궁극기 충전\n" +
                       "궁극기 활성 중: 피해 배율 ×" + LevatineUltimateBuff.DMG_MULT;
            }

            @Override
            protected void activate(Hero hero, Char target, int cell) {
                if (target == null || !target.isAlive()) return;

                MoltenFlame flame     = hero.buff(MoltenFlame.class);
                LevatineUltimateBuff ultBuff = hero.buff(LevatineUltimateBuff.class);

                // 궁극기 활성 중 피해 배율 보정
                float dmgMult = (ultBuff != null) ? LevatineUltimateBuff.DMG_MULT : 1.0f;

                // 강화 모드 여부 판정
                boolean forceEnhanced = (ultBuff != null && ultBuff.skillEnhancedReady);
                boolean at4Stacks     = (flame != null && flame.stacks() >= MoltenFlame.MAX_STACKS);

                if (at4Stacks || forceEnhanced) {
                    // ── 강화 모드: 4스택 소모 분기 ──
                    if (forceEnhanced) ultBuff.skillEnhancedReady = false;
                    if (flame != null) flame.consumeAll();

                    int dmg = Math.round(hero.damageRoll() * SKILL_ENHANCED_MULT * dmgMult);
                    target.damage(dmg, hero, DamageType.HEAT);

                    if (target.isAlive()) {
                        // 강제 연소 (4스택 소모 기준)
                        Combustion.apply(target, MoltenFlame.MAX_STACKS);
                    }

                    // 궁극기 충전
                    if (hero.activeUltimate != null) {
                        hero.activeUltimate.addCharge(SKILL_ULT_CHARGE);
                    }

                } else {
                    // ── 기본 모드: 열기 피해 + 불꽃 +1스택 ──
                    int dmg = Math.round(hero.damageRoll() * SKILL_MULT * dmgMult);
                    target.damage(dmg, hero, DamageType.HEAT);

                    if (flame != null) flame.addStack();
                }
            }
        };
    }

    // ─────────────────────────────────────────────
    // 연계기: 불꽃 공명
    // ─────────────────────────────────────────────

    @Override public int baseCooldown() { return 3; } // TODO: 수치 확정

    @Override public String chainName()        { return "불꽃 공명"; }
    @Override public String chainDescription() {
        return "조건: 연소 or 부식 상태 적\n" +
               "효과: 열기 피해(×" + CHAIN_MULT + ") + 녹아내린 불꽃 +1스택 + 궁극기 충전";
    }

    /**
     * 연계기 조건: 적이 연소(Combustion) 또는 부식(ArtsCorrosion) 상태.
     */
    @Override
    public boolean chainCondition(Hero hero, Char target) {
        if (target == null || !target.isAlive()) return false;
        return target.buff(Combustion.class) != null
                || target.buff(ArtsCorrosion.class) != null;
    }

    /**
     * 연계기 효과: 열기 피해 + 녹아내린 불꽃 +1스택 + 궁극기 충전.
     */
    @Override
    public void activateChain(Hero hero, Char target) {
        if (target == null || !target.isAlive()) return;

        int dmg = Math.round(hero.damageRoll() * CHAIN_MULT);
        target.damage(dmg, hero, DamageType.HEAT);

        // 녹아내린 불꽃 +1스택 (메인일 때만 MoltenFlame 존재)
        MoltenFlame flame = hero.buff(MoltenFlame.class);
        if (flame != null) flame.addStack();

        // 궁극기 충전
        if (hero.activeUltimate != null) {
            hero.activeUltimate.addCharge(CHAIN_ULT_CHARGE);
        }
    }

    // ─────────────────────────────────────────────
    // 궁극기: 용광로의 불꽃 (자기 강화형)
    // ─────────────────────────────────────────────

    @Override
    public Ultimate ultimate() {
        return new Ultimate() {

            @Override public int maxCharge()  { return 100; } // TODO: 수치 확정
            @Override public String name()    { return "용광로의 불꽃"; }
            @Override public String description() {
                return "자기 강화 버프 " + LevatineUltimateBuff.DURATION + "턴:\n" +
                       "- 피해 배율 ×" + LevatineUltimateBuff.DMG_MULT + "\n" +
                       "- 매 " + LevatineUltimateBuff.FLAME_INTERVAL + "턴마다 불꽃 자동 +1스택\n" +
                       "- 배틀스킬 1회 강화 준비 (강화 모드 강제 발동)\n" +
                       "TODO: 사거리 +1칸 추가 예정";
            }

            @Override public boolean selfTarget() { return true; }

            @Override
            protected void activate(Hero hero, Char target, int cell) {
                // 기존 버프가 있으면 갱신(duration 리셋), 없으면 새로 부여
                LevatineUltimateBuff existing = hero.buff(LevatineUltimateBuff.class);
                if (existing != null) {
                    existing.detach();
                }
                Buff.affect(hero, LevatineUltimateBuff.class);
            }
        };
    }
}
