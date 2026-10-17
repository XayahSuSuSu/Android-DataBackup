/* Compile-time probe for the bundled tar integer limits.
   Include path matches libgnu: tar-config, tar-gnu-embedded, tar/gnu. */

#include <config.h>
#include <limits.h>
#include <stddef.h>
#include <stdint.h>

_Static_assert(BITSIZEOF_SIZE_T == sizeof (size_t) * CHAR_BIT,
               "BITSIZEOF_SIZE_T does not match size_t");
_Static_assert(BITSIZEOF_PTRDIFF_T == sizeof (ptrdiff_t) * CHAR_BIT,
               "BITSIZEOF_PTRDIFF_T does not match ptrdiff_t");
_Static_assert(SIZE_MAX == (size_t) -1,
               "SIZE_MAX does not match (size_t)-1");
_Static_assert(PTRDIFF_MAX
                   == (ptrdiff_t) ((((size_t) 1)
                                    << (sizeof (ptrdiff_t) * CHAR_BIT - 1))
                                   - 1),
               "PTRDIFF_MAX does not match the signed pointer-difference bound");
_Static_assert((size_t) PTRDIFF_MIN
                   == (((size_t) 1) << (sizeof (ptrdiff_t) * CHAR_BIT - 1)),
               "PTRDIFF_MIN does not match the signed pointer-difference bound");
