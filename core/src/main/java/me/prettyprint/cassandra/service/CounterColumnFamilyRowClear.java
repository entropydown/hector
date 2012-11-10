/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package me.prettyprint.cassandra.service;

import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.Serializer;
import me.prettyprint.hector.api.beans.HCounterColumn;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.SliceCounterQuery;

/**
 * Resets each counter counter column in the row to zero.  This class is subject to the limitations of counter columns.
 * See the <a href="http://wiki.apache.org/cassandra/Counters">Cassandra Wiki</a> for more information on the
 * limitations.
 *
 * @author thrykol
 */
public class CounterColumnFamilyRowClear<K, N> {
	private Keyspace keyspace;
	private Serializer<K> keySerializer;
	private Serializer<N> nameSerializer;
	private K rowKey;
	private String cf;
	private int mutateInterval = 100;

	public CounterColumnFamilyRowClear(Keyspace keyspace, Serializer<K> keySerializer, Serializer<N> nameSerializer) {
		this.keyspace = keyspace;
		this.keySerializer =keySerializer;
		this.nameSerializer = nameSerializer;
	}

	/**
	 * Column family the row is part of.
	 *
	 * @param cf Column family name
	 * @return <code>this</code>
	 */
	public CounterColumnFamilyRowClear setColumnFamily(String cf) {
		this.cf = cf;
		return this;
	}

	/**
	 * Key of the source row.
	 *
	 * @param rowKey Row key
	 * @return <code>this</code>
	 */
	public CounterColumnFamilyRowClear setRowKey(K rowKey) {
		this.rowKey = rowKey;
		return this;
	}

	/**
	 * Set the mutate interval.
	 *
	 * @param interval Mutation interval
	 * @return <code>this</code>
	 */
	public CounterColumnFamilyRowClear setMutateInterval(int interval) {
		this.mutateInterval = interval;
		return this;
	}

	/**
	 * Clear the counter columns.
	 */
	public void clear() {
		Mutator<K> mutator = HFactory.createMutator(this.keyspace, this.keySerializer, new BatchSizeHint(1, this.mutateInterval));

		SliceCounterQuery<K, N> query = HFactory.createCounterSliceQuery(this.keyspace, this.keySerializer, this.nameSerializer).
						setColumnFamily(this.cf).
						setKey(this.rowKey);

		SliceCounterIterator<K, N> iterator =
						new SliceCounterIterator<K, N>(query, null, (N) null, false, 2000);

		while(iterator.hasNext()) {
			HCounterColumn<N> column = iterator.next();
			mutator.incrementCounter(this.rowKey, this.cf, column.getName(), column.getValue() * -1);
		}

		mutator.execute();
	}
}
