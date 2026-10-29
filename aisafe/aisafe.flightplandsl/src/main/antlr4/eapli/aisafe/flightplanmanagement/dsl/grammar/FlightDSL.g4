grammar FlightDSL;

// =========================================================
// PARSER RULES
// =========================================================

flightPlanFile : flight+ EOF ;

flight
    : FLIGHT id=FLIGHT_ID LBRACE
        flightType
        route
        leg+
      RBRACE
    ;

flightType : TYPE COLON value=(REGULAR | CHARTER) ;

route
    : ROUTE LBRACE
        ORIGIN      COLON origin=airportCode
        DESTINATION COLON destination=airportCode
      RBRACE
    ;

leg
    : LEG legNumber=INT LBRACE
        departure
        arrival
        fuel
        segmentsBlock
      RBRACE
    ;

departure
    : DEPARTURE LBRACE
        AIRPORT  COLON airport=airportCode
        DATETIME COLON dt=DATETIME_LIT
      RBRACE
    ;

arrival
    : ARRIVAL LBRACE
        AIRPORT  COLON airport=airportCode
        DATETIME COLON dt=DATETIME_LIT
      RBRACE
    ;

fuel     : FUEL COLON quantity=number unit=fuelUnit ;
fuelUnit : KG_UNIT | L_UNIT ;

segmentsBlock : SEGMENTS LBRACE segment+ RBRACE ;

segment
    : SEGMENT LBRACE
        START COLON startCoords=coordinates
        END   COLON endCoords=coordinates
        slotsBlock
        wind
      RBRACE
    ;

slotsBlock : SLOTS LBRACE slot+ RBRACE ;

slot
    : SLOT LBRACE
        ALTITUDE COLON altitude=length
        WIDTH    COLON width=length
      RBRACE
    ;

coordinates : LPAREN lat=number COMMA lon=number RPAREN ;

length : value=number M_UNIT ;

wind
    : WIND LBRACE
        DIRECTION COLON direction=number
        SPEED     COLON speed=number MS_UNIT
      RBRACE
    ;

airportCode : IATA_CODE | ICAO_CODE ;

number : INT | FLOAT ;

// =========================================================
// LEXER RULES
// =========================================================

// --- Case-insensitive letter fragments ---
fragment A : [aA] ; fragment B : [bB] ; fragment C : [cC] ;
fragment D : [dD] ; fragment E : [eE] ; fragment F : [fF] ;
fragment G : [gG] ; fragment H : [hH] ; fragment I : [iI] ;
fragment J : [jJ] ; fragment K : [kK] ; fragment L : [lL] ;
fragment M : [mM] ; fragment N : [nN] ; fragment O : [oO] ;
fragment P : [pP] ; fragment Q : [qQ] ; fragment R : [rR] ;
fragment S : [sS] ; fragment T : [tT] ; fragment U : [uU] ;
fragment V : [vV] ; fragment W : [wW] ; fragment X : [xX] ;
fragment Y : [yY] ; fragment Z : [zZ] ;

// --- Structural keywords (BEFORE literal tokens) ---
FLIGHT      : F L I G H T ;
TYPE        : T Y P E ;
REGULAR     : R E G U L A R ;
CHARTER     : C H A R T E R ;
ROUTE       : R O U T E ;
ORIGIN      : O R I G I N ;
DESTINATION : D E S T I N A T I O N ;
LEG         : L E G ;
DEPARTURE   : D E P A R T U R E ;
ARRIVAL     : A R R I V A L ;
AIRPORT     : A I R P O R T ;
DATETIME    : D A T E T I M E ;
FUEL        : F U E L ;
SEGMENTS    : S E G M E N T S ;
SEGMENT     : S E G M E N T ;
START       : S T A R T ;
END         : E N D ;
SLOTS       : S L O T S ;
SLOT        : S L O T ;
ALTITUDE    : A L T I T U D E ;
WIDTH       : W I D T H ;
WIND        : W I N D ;
DIRECTION   : D I R E C T I O N ;
SPEED       : S P E E D ;

// --- Unit keywords (MS_UNIT before M_UNIT for longest-match clarity) ---
MS_UNIT : M '/' S ;
KG_UNIT : K G ;
M_UNIT  : M ;
L_UNIT  : L ;

// --- Literals (case-sensitive: must be uppercase) ---
FLIGHT_ID    : [A-Z][A-Z][0-9] [0-9]? [0-9]? [0-9]? [A-Z]? ;
DATETIME_LIT : [0-9][0-9][0-9][0-9] '-' [0-9][0-9] '-' [0-9][0-9]
               'T' [0-9][0-9] ':' [0-9][0-9] (':' [0-9][0-9])? ;
ICAO_CODE    : [A-Z][A-Z][A-Z][A-Z] ;
IATA_CODE    : [A-Z][A-Z][A-Z] ;

FLOAT : '-'? [0-9]+ '.' [0-9]+ ;
INT   : '-'? [0-9]+ ;

// --- Punctuation ---
LBRACE : '{' ;
RBRACE : '}' ;
LPAREN : '(' ;
RPAREN : ')' ;
LBRACK : '[' ;
RBRACK : ']' ;
COMMA  : ',' ;
COLON  : ':' ;

// --- Whitespace & comments ---
WS            : [ \t\r\n]+ -> skip ;
LINE_COMMENT  : '//' ~[\r\n]* -> skip ;
BLOCK_COMMENT : '/*' .*? '*/' -> skip ;
