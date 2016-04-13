#include "mpjdev_natmpjdev_shared.h"
#include "mpi.h"


///////////////////////
// Utility Functions //
///////////////////////

/**
 * Get the corresponding MPI_Datatype for a MPJ datatype code
 * @param  MPJDatatype MPJ data type code
 * @return             Corresponding MPI_Datatype
 */
MPI_Datatype convertToMPIDataType(int MPJDatatype) {
    // if(MPJDatatype == MPJ_UNDEFINED) return MPI_UNDEFINED;
    // if(MPJDatatype == MPJ_NULL)      return MPI_NULL;
    if (MPJDatatype == MPJ_BYTE)      return MPI_BYTE;
    if (MPJDatatype == MPJ_CHAR)      return MPI_CHAR;
    if (MPJDatatype == MPJ_SHORT)     return MPI_SHORT;
    if (MPJDatatype == MPJ_BOOLEAN)   return MPI_C_BOOL;
    if (MPJDatatype == MPJ_INT)       return MPI_INT;
    if (MPJDatatype == MPJ_LONG)      return MPI_LONG;
    if (MPJDatatype == MPJ_FLOAT)     return MPI_FLOAT;
    if (MPJDatatype == MPJ_DOUBLE)    return MPI_DOUBLE;
    if (MPJDatatype == MPJ_PACKED)    return MPI_PACKED;
}

/**
 * Get the corresponding MPI_Op for a MPJ Op Code
 * @param  MPJOpCode MPJ OpCode to lookup
 * @return           MPI_Op for the given OpCode
 */
MPI_Op convertToMPIOp(int MPJOpCode) {
    if (MPJOpCode == MPJ_MAX_CODE)    return MPI_MAX;
    if (MPJOpCode == MPJ_MIN_CODE)    return MPI_MIN;
    if (MPJOpCode == MPJ_SUM_CODE)    return MPI_SUM;
    if (MPJOpCode == MPJ_PROD_CODE)   return MPI_PROD;
    if (MPJOpCode == MPJ_LAND_CODE)   return MPI_LAND;
    if (MPJOpCode == MPJ_BAND_CODE)   return MPI_BAND;
    if (MPJOpCode == MPJ_LOR_CODE)    return MPI_LOR;
    if (MPJOpCode == MPJ_BOR_CODE)    return MPI_BOR;
    if (MPJOpCode == MPJ_LXOR_CODE)   return MPI_LXOR;
    if (MPJOpCode == MPJ_BXOR_CODE)   return MPI_BXOR;
    if (MPJOpCode == MPJ_MAXLOC_CODE) return MPI_MAXLOC;
    if (MPJOpCode == MPJ_MINLOC_CODE) return MPI_MINLOC;
}

/**
 * Convert from MPJ assertion to the MPI equivalent
 * @param  MPJAssertion MPJ Assertion
 * @return              Equivalent RMA MPI assertion
 */
int convertToMPIAssert(int MPJAssertion) {
    int out = 0;
    if (MPJAssertion & MPJ_MODE_NOCHECK)   out = out | MPI_MODE_NOCHECK;
    if (MPJAssertion & MPJ_MODE_NOSTORE)   out = out | MPI_MODE_NOSTORE;
    if (MPJAssertion & MPJ_MODE_NOPUT)     out = out | MPI_MODE_NOPUT;
    if (MPJAssertion & MPJ_MODE_NOPRECEDE) out = out | MPI_MODE_NOPRECEDE;
    if (MPJAssertion & MPJ_MODE_NOSUCCEED) out = out | MPI_MODE_NOSUCCEED;
    return out;
}

/**
 * Convert from MPI Windows flavor constant to MPJ one
 * @param  MPIWinFlavor input
 * @return              MPJ window flavor const
 */
int convertToMPJWinFlavor(int MPIWinFlavor) {
    if (MPIWinFlavor == MPI_WIN_FLAVOR_CREATE)   return MPJ_WIN_FLAVOR_CREATE;
    if (MPIWinFlavor == MPI_WIN_FLAVOR_ALLOCATE) return MPJ_WIN_FLAVOR_ALLOCATE;
    if (MPIWinFlavor == MPI_WIN_FLAVOR_DYNAMIC)  return MPJ_WIN_FLAVOR_DYNAMIC;
    if (MPIWinFlavor == MPI_WIN_FLAVOR_SHARED)   return MPJ_WIN_FLAVOR_SHARED;
}

/**
 * Convert from MPI Win Model const to MPJ one
 * @param  MPIWinModel input
 * @return             MPJ Window Model const
 */
int convertToMPJWinModel(int MPIWinModel) {
    if (MPIWinModel == MPI_WIN_SEPARATE) return MPJ_WIN_SEPARATE;
    if (MPIWinModel == MPI_WIN_UNIFIED)  return MPJ_WIN_UNIFIED;
}
