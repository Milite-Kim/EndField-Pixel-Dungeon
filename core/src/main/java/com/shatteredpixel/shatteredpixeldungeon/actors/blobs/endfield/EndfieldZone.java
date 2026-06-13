/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.blobs.endfield;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

/**
 * 엔드필드 지대 (EndfieldZone) — 고정 범위 지속 피해 장판의 공통 베이스
 *
 * SPD의 {@link Blob}(가스 확산 모델)을 상속하되, 확산하지 않는 고정 발자국(footprint)으로
 * 동작하도록 evolve()를 재정의한다. 엔드필드 오퍼레이터들의 "냉기 지대 / 중력 특이점 /
 * 꽁꽁이" 등 "n턴 간 매 턴 피해, 종료 시 폭발" 류 효과의 공통 토대.
 *
 * [동작]
 *  - cur[cell] = 해당 칸의 남은 지속 턴 수 (모든 칸을 동일 duration으로 시드).
 *  - 매 턴(evolve): 활성 칸 위의 적에게 tickDamage 적용 + onTick() 훅 → cur[cell] 1 감소.
 *  - 마지막 턴(지대 전체가 0이 되는 턴): explosionDamage > 0 이면 폭발 → 발자국 위 적에게
 *    explosionDamage 적용 + onExplode() 훅.
 *
 * [피해 출처]
 *  지대 피해는 항상 {@code Dungeon.hero}를 src로 사용한다. 따라서
 *  ArtsAmplification 등 "src instanceof Hero" 기반 증폭 버프가 정상 적용되며,
 *  적(ENEMY)에게만 피해가 들어간다(아군/Hero 무피해).
 *
 * [생성]
 *  {@link #summon(Class, int, int, int, int, int)} 정적 헬퍼로 발자국 시드 + 씬 등록까지 처리.
 *
 * [서브클래스 책임]
 *  - {@link #damageType()} : 지대 피해 속성 (필수)
 *  - {@link #onTick(Char)} / {@link #onExplode(Char)} : 상태이상 등 부가 효과 (선택)
 *  - {@link #use(com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter)} : 파티클 연출 (선택)
 */
public abstract class EndfieldZone extends Blob {

    /** 매 턴 적 1명에게 가하는 피해 (시전 시점에 hero.damageRoll 기반으로 계산해 주입). */
    protected int tickDamage = 0;

    /** 종료 시 폭발 피해 (0 = 폭발 없음). */
    protected int explosionDamage = 0;

    // ─────────────────────────────────────────────
    // 서브클래스 훅
    // ─────────────────────────────────────────────

    /** 지대 피해 속성. */
    protected abstract DamageType damageType();

    /** 매 턴 지대 위 적에게 피해를 준 직후 호출 (감속/부착 등 부가 효과). 기본 no-op. */
    protected void onTick(Char ch) {}

    /** 종료 폭발이 적에게 피해를 준 직후 호출 (강제 동결/부착 등). 기본 no-op. */
    protected void onExplode(Char ch) {}

    // ─────────────────────────────────────────────
    // 피해 수치 주입 (체이너블)
    // ─────────────────────────────────────────────

    public EndfieldZone setup(int tickDamage, int explosionDamage) {
        this.tickDamage = tickDamage;
        this.explosionDamage = explosionDamage;
        return this;
    }

    // ─────────────────────────────────────────────
    // 고정 발자국 진행 (확산 없음)
    // ─────────────────────────────────────────────

    @Override
    protected void evolve() {
        int w = Dungeon.level.width();
        ArrayList<Integer> expiring = new ArrayList<>();
        int cell;

        for (int i = area.top - 1; i <= area.bottom; i++) {
            for (int j = area.left - 1; j <= area.right; j++) {
                cell = j + i * w;
                if (!Dungeon.level.insideMap(cell)) continue;

                int turns = cur[cell];
                if (turns > 0) {
                    // 매 턴 피해 + 부가 효과
                    damageEnemyOn(cell, tickDamage, false);

                    int next = turns - 1;
                    if (next <= 0) {
                        // 이번 턴에 만료되는 칸
                        expiring.add(cell);
                        off[cell] = 0;
                    } else {
                        off[cell] = next;
                        area.union(j, i);
                    }
                    volume += off[cell];
                } else {
                    off[cell] = 0;
                }
            }
        }

        // 지대 전체가 이번 턴에 소멸 → 폭발
        if (volume == 0 && !expiring.isEmpty() && explosionDamage > 0) {
            for (int c : expiring) {
                damageEnemyOn(c, explosionDamage, true);
            }
        }
    }

    /** 해당 칸 위의 적에게 피해 + 훅 호출. 아군/Hero/빈 칸은 무시. */
    private void damageEnemyOn(int cell, int dmg, boolean explosion) {
        Char ch = Actor.findChar(cell);
        if (ch == null || !ch.isAlive()) return;
        if (ch.alignment != Char.Alignment.ENEMY) return;

        if (dmg > 0 && Dungeon.hero != null) {
            ch.damage(dmg, Dungeon.hero, damageType());
        }
        if (!ch.isAlive()) return;

        if (explosion) onExplode(ch);
        else           onTick(ch);
    }

    // ─────────────────────────────────────────────
    // 생성 헬퍼
    // ─────────────────────────────────────────────

    /**
     * 지대를 소환한다. center 기준 반경 radius(체비셰프) 이내의 비-solid 칸을 duration 턴
     * 동안 활성화하고, 씬에 등록한다.
     *
     * @param type            구체 지대 클래스
     * @param center          중심 칸
     * @param radius          반경 (0 = 중심 1칸만, 1 = 3×3 ...)
     * @param duration        지속 턴 수
     * @param tickDamage      매 턴 피해 (시전 시 계산된 고정값)
     * @param explosionDamage 종료 폭발 피해 (0 = 폭발 없음)
     * @return 소환된 지대 인스턴스 (시드된 칸이 없으면 null)
     */
    public static <T extends EndfieldZone> T summon(Class<T> type, int center, int radius,
                                                    int duration, int tickDamage, int explosionDamage) {
        if (duration <= 0) return null;

        T zone = null;
        int len = Dungeon.level.length();
        for (int c = 0; c < len; c++) {
            if (Dungeon.level.solid[c]) continue;
            if (Dungeon.level.distance(center, c) <= radius) {
                zone = Blob.seed(c, duration, type);
            }
        }

        if (zone != null) {
            zone.setup(tickDamage, explosionDamage);
            GameScene.add(zone);
        }
        return zone;
    }

    // ─────────────────────────────────────────────
    // 저장/불러오기
    // ─────────────────────────────────────────────

    private static final String TICK_DAMAGE = "tickDamage";
    private static final String EXPL_DAMAGE  = "explosionDamage";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(TICK_DAMAGE, tickDamage);
        bundle.put(EXPL_DAMAGE, explosionDamage);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        tickDamage = bundle.getInt(TICK_DAMAGE);
        explosionDamage = bundle.getInt(EXPL_DAMAGE);
    }
}
