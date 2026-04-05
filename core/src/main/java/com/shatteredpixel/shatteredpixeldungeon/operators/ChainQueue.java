/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.operators;

import com.watabou.utils.Bundle;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 연계기 큐 (ChainQueue)
 *
 * 팀 오퍼레이터들의 연계기 대기열을 관리한다.
 *
 * [규칙]
 * 1. 조건이 먼저 충족된 연계기가 큐의 앞에 위치한다.
 * 2. 동일 타이밍에 조건이 충족된 경우 팀 파티 순서가 빠른 오퍼레이터 우선
 *    (호출자가 teamOperators 순서대로 enqueue() 하는 것으로 보장됨)
 * 3. 이미 큐에 있는 오퍼레이터의 조건이 재충족되면,
 *    현재 위치를 유지한 채 타이머만 갱신된다 (순서 변동 없음).
 * 4. 타이머가 0이 되면 해당 엔트리는 자동 폐기된다.
 * 5. UI에는 큐의 헤드(peek())만 노출된다.
 */
public class ChainQueue {

    /** 기본 연계기 유효 턴 수. TODO: 수치 확정 */
    public static final int DEFAULT_WINDOW = 3;

    // ─────────────────────────────────────────────
    // 엔트리
    // ─────────────────────────────────────────────

    public static class Entry {
        public final TeamOperator operator;
        public int remainingTurns;

        Entry(TeamOperator op, int turns) {
            operator      = op;
            remainingTurns = turns;
        }
    }

    private final ArrayList<Entry> queue = new ArrayList<>();

    // ─────────────────────────────────────────────
    // 큐 조작
    // ─────────────────────────────────────────────

    /**
     * 오퍼레이터의 연계기를 큐에 추가한다.
     * 이미 큐에 있으면 해당 위치에서 타이머를 DEFAULT_WINDOW로 갱신한다.
     */
    public void enqueue(TeamOperator op) {
        enqueue(op, DEFAULT_WINDOW);
    }

    /**
     * 오퍼레이터의 연계기를 큐에 추가한다.
     * 이미 큐에 있으면 해당 위치에서 타이머를 turns로 갱신한다.
     */
    public void enqueue(TeamOperator op, int turns) {
        for (Entry e : queue) {
            if (e.operator == op) {
                e.remainingTurns = turns; // 현재 위치 유지, 타이머만 갱신
                return;
            }
        }
        queue.add(new Entry(op, turns));
    }

    /**
     * 매 턴 호출. 모든 엔트리의 남은 턴을 1 감소시키고 만료된 항목을 폐기한다.
     */
    public void tick() {
        Iterator<Entry> it = queue.iterator();
        while (it.hasNext()) {
            Entry e = it.next();
            e.remainingTurns--;
            if (e.remainingTurns <= 0) {
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
    // 팀 오퍼레이터 인스턴스는 Hero.teamOperators 에 이미 저장되므로,
    // 여기서는 클래스명 + 남은 턴만 저장하고, 복원 시 teamOperators 에서 조회한다.
    // ─────────────────────────────────────────────

    private static final String CHAIN_QUEUE       = "chainQueue";
    private static final String ENTRY_CLASS       = "class";
    private static final String ENTRY_TURNS       = "turns";

    public void storeInBundle(Bundle bundle) {
        ArrayList<Bundle> entries = new ArrayList<>();
        for (Entry e : queue) {
            Bundle eb = new Bundle();
            eb.put(ENTRY_CLASS, e.operator.getClass().getName());
            eb.put(ENTRY_TURNS, e.remainingTurns);
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
            String className    = eb.getString(ENTRY_CLASS);
            int    remaining    = eb.getInt(ENTRY_TURNS);
            for (TeamOperator op : teamOperators) {
                if (op.getClass().getName().equals(className)) {
                    queue.add(new Entry(op, remaining));
                    break;
                }
            }
        }
    }
}
