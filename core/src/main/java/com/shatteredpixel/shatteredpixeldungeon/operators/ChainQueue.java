/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.operators;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.watabou.utils.Bundle;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 연계기 큐 (ChainQueue)
 *
 * 팀 오퍼레이터들의 연계기 대기열을 관리한다.
 *
 * [타이머 방식]
 * Actor.now() 기반 절대 게임 시간으로 만료를 판단한다.
 * 이속·공속 버프와 무관하게, 연계기가 활성화된 시점으로부터
 * 정확히 DEFAULT_WINDOW 게임 시간 단위 후에 만료된다.
 *   예) Actor.now()=2.2에 활성화 → DEFAULT_WINDOW=3 이면 5.2에 만료
 *
 * [큐 순서 규칙]
 * 1. 조건이 먼저 충족된 연계기가 큐의 앞에 위치한다.
 * 2. 동일 타이밍에 조건이 충족된 경우 팀 파티 순서가 빠른 오퍼레이터 우선
 *    (호출자가 teamOperators 순서대로 enqueue() 하는 것으로 보장됨)
 * 3. 이미 큐에 있는 오퍼레이터의 조건이 재충족되면,
 *    현재 위치를 유지한 채 만료 시간만 갱신된다 (순서 변동 없음).
 * 4. 만료 시간이 지나면 해당 엔트리는 자동 폐기된다.
 * 5. UI에는 큐의 헤드(peek())만 노출된다.
 */
public class ChainQueue {

    /** 기본 연계기 유효 시간 (게임 시간 단위, 1 = 표준 1턴). TODO: 수치 확정 */
    public static final float DEFAULT_WINDOW = 3f;

    // ─────────────────────────────────────────────
    // 엔트리
    // ─────────────────────────────────────────────

    public static class Entry {
        public final TeamOperator operator;
        /** 이 연계기가 만료되는 절대 게임 시간 (Actor.now() 기준) */
        public float expiresAt;

        Entry(TeamOperator op, float expiresAt) {
            this.operator  = op;
            this.expiresAt = expiresAt;
        }
    }

    private final ArrayList<Entry> queue = new ArrayList<>();

    // ─────────────────────────────────────────────
    // 큐 조작
    // ─────────────────────────────────────────────

    /**
     * 오퍼레이터의 연계기를 큐에 추가한다.
     * 만료 시간 = Actor.now() + DEFAULT_WINDOW
     * 이미 큐에 있으면 해당 위치에서 만료 시간만 갱신한다.
     */
    public void enqueue(TeamOperator op) {
        enqueue(op, DEFAULT_WINDOW);
    }

    /**
     * 오퍼레이터의 연계기를 큐에 추가한다.
     * 만료 시간 = Actor.now() + window
     * 이미 큐에 있으면 해당 위치에서 만료 시간만 갱신한다.
     */
    public void enqueue(TeamOperator op, float window) {
        float expiresAt = Actor.now() + window;
        for (Entry e : queue) {
            if (e.operator == op) {
                e.expiresAt = expiresAt; // 현재 위치 유지, 만료 시간만 갱신
                return;
            }
        }
        queue.add(new Entry(op, expiresAt));
    }

    /**
     * 매 Hero 행동 완료 시 호출.
     * Actor.now() 이후가 된 엔트리(만료)를 폐기한다.
     */
    public void tick() {
        float now = Actor.now();
        Iterator<Entry> it = queue.iterator();
        while (it.hasNext()) {
            if (it.next().expiresAt <= now) {
                it.remove();
            }
        }
    }

    /**
     * 현재 활성 연계기 엔트리 (큐 헤드). UI 버튼에 표시된다.
     * null이면 대기 중인 연계기 없음.
     */
    public Entry peek() {
        return queue.isEmpty() ? null : queue.get(0);
    }

    /**
     * 큐 헤드를 꺼내 반환한다 (발동 시 호출).
     * null이면 큐가 비어 있음.
     */
    public TeamOperator consume() {
        if (queue.isEmpty()) return null;
        return queue.remove(0).operator;
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    // ─────────────────────────────────────────────
    // 저장/불러오기
    // Actor.now()는 저장/불러오기 이후에도 연속적이므로
    // expiresAt을 그대로 저장하면 복원 후에도 정확한 만료 시간이 유지된다.
    // ─────────────────────────────────────────────

    private static final String CHAIN_QUEUE  = "chainQueue";
    private static final String ENTRY_CLASS  = "class";
    private static final String ENTRY_EXPIRY = "expiresAt";

    public void storeInBundle(Bundle bundle) {
        ArrayList<Bundle> entries = new ArrayList<>();
        for (Entry e : queue) {
            Bundle eb = new Bundle();
            eb.put(ENTRY_CLASS,  e.operator.getClass().getName());
            eb.put(ENTRY_EXPIRY, e.expiresAt);
            entries.add(eb);
        }
        bundle.put(CHAIN_QUEUE, entries.toArray(new Bundle[0]));
    }

    /**
     * @param teamOperators Hero.teamOperators — 인스턴스 조회용
     */
    public void restoreFromBundle(Bundle bundle, List<TeamOperator> teamOperators) {
        queue.clear();
        Bundle[] entries = bundle.getBundleArray(CHAIN_QUEUE);
        if (entries == null) return;
        for (Bundle eb : entries) {
            String className = eb.getString(ENTRY_CLASS);
            float  expiresAt = eb.getFloat(ENTRY_EXPIRY);
            // 이미 만료된 엔트리는 복원하지 않는다
            if (expiresAt <= Actor.now()) continue;
            for (TeamOperator op : teamOperators) {
                if (op.getClass().getName().equals(className)) {
                    queue.add(new Entry(op, expiresAt));
                    break;
                }
            }
        }
    }
}
