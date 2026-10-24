#include <config.h>

#include <limits.h>
#include <stddef.h>
#include <stdint.h>

#define LITERAL_SUFFIX_(value, suffix) value ## suffix
#define LITERAL_SUFFIX(value, suffix) LITERAL_SUFFIX_ (value, suffix)

_Static_assert (BITSIZEOF_SIZE_T == sizeof (size_t) * CHAR_BIT,
                "size_t width must match the target ABI");
_Static_assert (BITSIZEOF_PTRDIFF_T == sizeof (ptrdiff_t) * CHAR_BIT,
                "ptrdiff_t width must match the target ABI");
_Static_assert (SIZE_MAX == (size_t) -1,
                "SIZE_MAX must match the target ABI");
_Static_assert (PTRDIFF_MAX == __PTRDIFF_MAX__,
                "PTRDIFF_MAX must match the target ABI");
_Static_assert (PTRDIFF_MIN == -__PTRDIFF_MAX__ - 1,
                "PTRDIFF_MIN must match the target ABI");
_Static_assert (_Generic (LITERAL_SUFFIX (0, SIZE_T_SUFFIX),
                          size_t: 1, default: 0),
                "SIZE_T_SUFFIX must produce a size_t literal");
_Static_assert (_Generic (LITERAL_SUFFIX (0, PTRDIFF_T_SUFFIX),
                          ptrdiff_t: 1, default: 0),
                "PTRDIFF_T_SUFFIX must produce a ptrdiff_t literal");
