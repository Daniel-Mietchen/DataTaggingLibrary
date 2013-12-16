package edu.harvard.iq.datatags.parser.flowcharts.references;

import java.util.Objects;

/**
 * A reference to a node in the parsed AST.
 * @author michael
 */
public class NodeReference {
	private final String id;
	private final NodeType type;

	public NodeReference(String id, NodeType type) {
		this.id = id;
		this.type = type;
	}

	public String getId() {
		return id;
	}

	public NodeType getType() {
		return type;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 29 * hash + Objects.hashCode(this.id);
		hash = 29 * hash + Objects.hashCode(this.type);
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final NodeReference other = (NodeReference) obj;
		if (!Objects.equals(this.id, other.id)) {
			return false;
		}
		return this.type == other.type;
	}
	
	
	
}