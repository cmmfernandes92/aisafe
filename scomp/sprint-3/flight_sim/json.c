/**
 * JSON parser for the flight simulator format; walks the known schema.
 * Extracts raw values only; defaults/projection/validation live in config.c.
 **/

#include"types.h"
#include"json.h"

#include<stdio.h>
#include<stdlib.h>
#include<string.h>
#include<ctype.h>

/*
 * Read the whole file into a buffer, then scan it with helpers that find a
 * "key" and read the value after it.
 */

#define MAX_JSON_SIZE (8 * 1024 * 1024)

static char *read_whole_file(const char *path){
    FILE *fp;
    char *buf;
    size_t n;

    fp = fopen(path, "r");
    if(fp == NULL){
        perror("fopen json");
        return NULL;
    }

    buf = malloc(MAX_JSON_SIZE);
    if(buf == NULL){
        perror("malloc json buffer");
        fclose(fp);
        return NULL;
    }

    n = fread(buf, 1, MAX_JSON_SIZE - 1, fp);
    buf[n] = '\0';
    fclose(fp);
    return buf;
}

/*
 * Find the next occurrence of key starting at *from.
 * Returns pointer just past the colon after the key, or NULL if not found.
 */
static const char *find_key(const char *from, const char *key){
    char quoted[128];
    const char *p;

    snprintf(quoted, sizeof(quoted), "\"%s\"", key);
    p = strstr(from, quoted);
    if(p == NULL)
        return NULL;

    p += strlen(quoted);
    /* skip whitespace and the colon */
    while(*p && (isspace((unsigned char)*p) || *p == ':'))
        p++;
    return p;
}

/* Read a double value at p. Returns 0 on success, writes via out. */
static int read_double_at(const char *p, double *out){
    char *end;
    double v;

    if(p == NULL)
        return -1;
    v = strtod(p, &end);
    if(end == p)
        return -1;
    *out = v;
    return 0;
}

/* Read an int value at p. Returns 0 on success. */
static int read_int_at(const char *p, int *out){
    char *end;
    long v;

    if(p == NULL)
        return -1;
    v = strtol(p, &end, 10);
    if(end == p)
        return -1;
    *out = (int)v;
    return 0;
}

/*
 * Read a quoted string value at p into dest (size bytes). p must point at the
 * opening quote (after find_key). Returns 0 on success.
 */
static int read_string_at(const char *p, char *dest, size_t size){
    size_t i;

    if(p == NULL || *p != '"')
        return -1;
    p++;
    i = 0;
    while(*p && *p != '"' && i < size - 1)
        dest[i++] = *p++;
    dest[i] = '\0';
    return 0;
}

/*
 * Parse the sim_params block. Looks up each expected key relative to the
 * start of the sim_params object. Missing keys left untouched; config.c applies defaults.
 */
static void parse_sim_params(const char *text, sim_params_t *params){
    const char *block;
    const char *p;
    int iv;
    double dv;

    block = find_key(text, "sim_params");
    if(block == NULL)
        return;

    if((p = find_key(block, "total_steps")) && read_int_at(p, &iv) == 0)
        params->total_steps = iv;
    if((p = find_key(block, "max_violations")) && read_int_at(p, &iv) == 0)
        params->max_violations = iv;
    if((p = find_key(block, "seconds_per_step")) && read_double_at(p, &dv) == 0)
        params->seconds_per_step = dv;
    if((p = find_key(block, "history_depth")) && read_int_at(p, &iv) == 0)
        params->history_depth = iv;
    if((p = find_key(block, "h_min")) && read_double_at(p, &dv) == 0)
        params->h_min = dv;
    if((p = find_key(block, "v_min")) && read_double_at(p, &dv) == 0)
        params->v_min = dv;
    if((p = find_key(block, "area_x_min")) && read_double_at(p, &dv) == 0)
        params->area_x_min = dv;
    if((p = find_key(block, "area_x_max")) && read_double_at(p, &dv) == 0)
        params->area_x_max = dv;
    if((p = find_key(block, "area_y_min")) && read_double_at(p, &dv) == 0)
        params->area_y_min = dv;
    if((p = find_key(block, "area_y_max")) && read_double_at(p, &dv) == 0)
        params->area_y_max = dv;
    if((p = find_key(block, "area_alt_min")) && read_double_at(p, &dv) == 0)
        params->area_alt_min = dv;
    if((p = find_key(block, "area_alt_max")) && read_double_at(p, &dv) == 0)
        params->area_alt_max = dv;
    if((p = find_key(block, "ref_lat")) && read_double_at(p, &dv) == 0)
        params->ref_lat = dv;
    if((p = find_key(block, "ref_lon")) && read_double_at(p, &dv) == 0)
        params->ref_lon = dv;
    if((p = find_key(block, "name")))
        read_string_at(p, params->scenario, SCENARIO_NAME_SIZE);
}

/*
 * Parse one profile table (climb/descend) inside a flight.
 * cursor points inside the flight object; key is "Climb" or "Descend".
 * Reads up to MAX_PROFILE rows of { Altitude, Speed, Rate* }. Returns count.
 */
static int parse_profile(const char *cursor, const char *key,
                        const char *rate_key, profile_point_t out[]){
    const char *block;
    const char *p;
    int count;

    block = find_key(cursor, key);
    if(block == NULL)
        return 0;

    count = 0;
    p = block;
    while(count < MAX_PROFILE){
        const char *alt_p;
        const char *spd_p;
        const char *rate_p;
        const char *inner;
        double alt;
        double spd;
        double rate;

        /* find the next "Altitude" within this profile array; stop at ']' */
        alt_p = strstr(p, "\"Altitude\"");
        if(alt_p == NULL)
            break;
        /* if a closing bracket of the array comes before the next row, stop */
        {
            const char *bracket;
            bracket = strchr(p, ']');
            if(bracket != NULL && bracket < alt_p)
                break;
        }

        inner = find_key(alt_p, "Value");
        if(read_double_at(inner, &alt) != 0)
            break;

        spd_p = find_key(alt_p, "Speed");
        inner = (spd_p != NULL) ? find_key(spd_p, "Value") : NULL;
        if(read_double_at(inner, &spd) != 0)
            spd = 0.0;

        rate_p = find_key(alt_p, rate_key);
        inner = (rate_p != NULL) ? find_key(rate_p, "Value") : NULL;
        if(read_double_at(inner, &rate) != 0)
            rate = 0.0;

        out[count].altitude = alt;
        out[count].speed_knots = spd;
        out[count].rate = rate;
        count++;

        /* advance past this row */
        p = (rate_p != NULL) ? rate_p : (spd_p != NULL ? spd_p : alt_p) ;
        p += 1;
    }

    return count;
}

/*
 * Parse the Segments array of a flight. Reads Start/End lat/long/alt for each
 * segment, projecting is left to config.c. We store raw geographic values in
 * temporary fields; config.c projects them to x/y. Returns segment count.
 *
 * For simplicity at parse time we store geographic start/end into the segment
 * struct's raw lat/lon/alt fields; config.c fills end_x/end_y via projection.
 */
static int parse_segments(const char *cursor, flight_t *flight){
    const char *block;
    const char *p;
    int count;

    block = find_key(cursor, "Segments");
    if(block == NULL)
        return 0;

    count = 0;
    p = block;
    while(count < MAX_SEGMENTS){
        const char *start_p;
        const char *end_p;
        const char *inner;
        segment_t *seg;

        start_p = strstr(p, "\"Start\"");
        if(start_p == NULL)
            break;
        {
            const char *bracket;
            bracket = strchr(p, ']');
            /* a flight object ends; the Segments array close bracket */
            if(bracket != NULL && bracket < start_p)
                break;
        }

        end_p = strstr(start_p, "\"End\"");
        if(end_p == NULL)
            break;

        seg = &flight->segments[count];

        inner = find_key(start_p, "Latitude");
        read_double_at(inner, &seg->start_lat);
        inner = find_key(start_p, "Longitude");
        read_double_at(inner, &seg->start_lon);
        inner = find_key(start_p, "Altitude");
        inner = (inner != NULL) ? find_key(inner, "Quantity") : NULL;
        read_double_at(inner, &seg->start_alt);

        inner = find_key(end_p, "Latitude");
        read_double_at(inner, &seg->end_lat);
        inner = find_key(end_p, "Longitude");
        read_double_at(inner, &seg->end_lon);
        inner = find_key(end_p, "Altitude");
        inner = (inner != NULL) ? find_key(inner, "Quantity") : NULL;
        read_double_at(inner, &seg->end_alt);

        count++;
        p = end_p + 1;
    }

    return count;
}

/*
 * Parse the flights[] array; for each flight: ID, climb and descend profiles,
 * and the segments. Flight index is assigned by order.
 * Returns 0 on success and writes *n_flights.
 */
static int parse_flights(const char *text, flight_t flights[],
                        int *n_flights){
    const char *arr;
    const char *p;
    int count;

    arr = find_key(text, "flights");
    if(arr == NULL){
        fprintf(stderr, "json: missing flights array\n");
        return -1;
    }

    count = 0;
    p = arr;
    while(count < MAX_FLIGHTS){
        const char *obj;
        const char *idp;
        flight_t *f;
        int iv;

        /* each flight object begins at the next '{' that contains an "ID" */
        idp = find_key(p, "ID");
        if(idp == NULL)
            break;

        obj = idp;
        f = &flights[count];
        memset(f, 0, sizeof(flight_t));

        f->index = count;
        if(read_int_at(idp, &iv) == 0)
            f->id = iv;

        // optional non simultatenous entry; default 0 (zeroed by memset).
        
        {
            const char *limit = strstr(idp + 1, "\"ID\"");
            const char *ep = find_key(obj, "EntryStep");
            int ev;
            if(ep != NULL && (limit == NULL || ep < limit)
                    && read_int_at(ep, &ev) == 0 && ev > 0)
                f->entry_step = ev;
        }

        f->n_climb = parse_profile(obj, "Climb", "RateClimb", f->climb);
        f->n_descend = parse_profile(obj, "Descend", "RateDescent",
                                     f->descend);
        f->n_segments = parse_segments(obj, f);
        f->seg_index = 0;

        count++;

        /* advance past this flight: jump beyond its segments block */
        {
            const char *next;
            next = strstr(idp + 1, "\"ID\"");
            if(next == NULL)
                break;
            p = next;
        }
    }

    *n_flights = count;
    return 0;
}

int json_parse_file(const char *path, sim_params_t *params,
                    flight_t flights[], int *n_flights){
    char *text;
    int rc;

    text = read_whole_file(path);
    if(text == NULL)
        return -1;

    parse_sim_params(text, params);
    rc = parse_flights(text, flights, n_flights);

    free(text);
    return rc;
}
