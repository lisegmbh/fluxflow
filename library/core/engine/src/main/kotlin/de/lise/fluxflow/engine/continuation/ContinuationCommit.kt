package de.lise.fluxflow.engine.continuation

fun interface ContinuationCommit {
    fun commit()
    
    operator fun plus(other: ContinuationCommit): ContinuationCommit {
        if(other == Nop) {
            return this
        }
        if(this == Nop) {
            return other
        }
        return ContinuationCommit { 
            this.commit()
            other.commit()
        }
    }
    
    companion object {
        val Nop: ContinuationCommit = ContinuationCommit {  }
    }
}