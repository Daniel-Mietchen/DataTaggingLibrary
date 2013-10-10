package edu.harvard.iq.datatags.model.values;

import edu.harvard.iq.datatags.model.types.AggregateType;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A tag value that can contain a varying amount of items.
 * Value equality is based on type and contained values - <em>not</em> on the name.
 * This is the same as in programming languages, where the name of the variable
 * is not part of the equality test.
 * 
 * @author michael
 */
public class AggregateValue extends TagValue<AggregateType>{
	
	private final Set<TagValue> values = new HashSet<>();

	public AggregateValue(String name, AggregateType type, String info) {
		super(name, type, info);
	}
	
	public Set<TagValue> getValues() {
		return Collections.unmodifiableSet(values);
	}
	
	public void add( Collection<? extends TagValue> valueCollection ) {
		for ( TagValue v : valueCollection ) {
			add( v );
		}
	}
	
	public void add( TagValue tagValue ) {
		if ( ! tagValue.getType().equals(getType().getItemType()) ) {
			throw new IllegalArgumentException( "Added value type ("+tagValue.getType()+") "
					+ "different from aggregated type (" + getType().getItemType() + ")");
					
		}
		values.add( tagValue );
	}
	
	@Override
	public <R> R  accept(edu.harvard.iq.datatags.model.values.TagValue.Visitor<R> tv) {
		return tv.visitAggregateValue(this);
	}
	
	@Override
	public AggregateValue getOwnableInstance() {
		AggregateValue copy = new AggregateValue(getName(), getType(), getInfo());
		for ( TagValue v : values ) {
			copy.add( v.getOwnableInstance() );
		}
		return copy;
	}

	@Override
	public int hashCode() {
		int hash = 3;
		hash = 43 * hash + Objects.hashCode(this.values);
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if ( ! (obj instanceof AggregateValue)) {
			return false;
		}
		final AggregateValue other = (AggregateValue) obj;
		return super.equals(obj) &&
				Objects.equals(this.values, other.values);
	}
	
	
}
