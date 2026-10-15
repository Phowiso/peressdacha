/** Функциональный плоттер: команды возвращают новое состояние и сообщения.
 * Ввод состояния не изменяется; печать выполняется только в конце программы.
 */
type Point = number;
type Distance = number;
type Angle = number;
type Position = Readonly<{ x: Point; y: Point }>;
// Объекты вместо enum позволяют запускать файл напрямую в Node.js 22.18+.
const CarriageState = { UP: 0, DOWN: 1 } as const;
type CarriageState = typeof CarriageState[keyof typeof CarriageState];
const LineColor = { BLACK: "чорный", RED: "красный", GREEN: "зелёный" } as const;
type LineColor = typeof LineColor[keyof typeof LineColor];
type PlotterState = Readonly<{
  position: Position;
  angle: Angle;
  color: LineColor;
  carriageState: CarriageState;
}>;
type Result = Readonly<{ state: PlotterState; messages: readonly string[] }>;
type Command = (state: PlotterState) => Result;
type Printer = (message: string) => void;

// Чистые вычисления: одинаковые аргументы всегда дают одинаковый результат.
function calcNewPosition(distance: Distance, angle: Angle, current: Position): Position {
  const radians = angle * Math.PI / 180;
  return {
    x: Math.round(current.x + distance * Math.cos(radians)),
    y: Math.round(current.y + distance * Math.sin(radians))
  };
}

function drawLine(from: Position, to: Position, color: LineColor): string {
  return `...Чертим линию из (${from.x}, ${from.y}) в (${to.x}, ${to.y}) используя ${color} цвет.`;
}

function move(distance: Distance): Command {
  return state => {
    const position = calcNewPosition(distance, state.angle, state.position);
    const message = state.carriageState === CarriageState.DOWN
      ? drawLine(state.position, position, state.color)
      : `Передвигаем на ${distance} от точки (${state.position.x}, ${state.position.y})`;
    return { state: { ...state, position }, messages: [message] };
  };
}

function turn(angle: Angle): Command {
  return state => ({
    state: { ...state, angle: (state.angle + angle) % 360 },
    messages: [`Поворачиваем на ${angle} градусов`]
  });
}

const carriageUp: Command = state => ({
  state: { ...state, carriageState: CarriageState.UP },
  messages: ["Поднимаем каретку"]
});

const carriageDown: Command = state => ({
  state: { ...state, carriageState: CarriageState.DOWN },
  messages: ["Опускаем каретку"]
});

function setColor(color: LineColor): Command {
  return state => ({
    state: { ...state, color },
    messages: [`Устанавливаем ${color} цвет линии.`]
  });
}

function setPosition(position: Position): Command {
  return state => ({
    state: { ...state, position: { ...position } },
    messages: [`Устанавливаем позицию каретки в (${position.x}, ${position.y}).`]
  });
}

// Композиция команд: reduce передаёт новое состояние следующей команде.
function sequence(commands: readonly Command[]): Command {
  return initialState => commands.reduce<Result>((result, command) => {
    const next = command(result.state);
    return {
      state: next.state,
      messages: [...result.messages, ...next.messages]
    };
  }, { state: initialState, messages: [] });
}

function drawPolygon(sides: number, size: Distance): Command {
  return sequence([
    carriageDown,
    ...Array.from({ length: sides }, () => sequence([
      move(size), turn(360 / sides)
    ])),
    carriageUp
  ]);
}

function drawTriangle(size: Distance): Command {
  return drawPolygon(3, size);
}

function drawSquare(size: Distance): Command {
  return drawPolygon(4, size);
}

function initializePlotterState(
  position: Position, angle: Angle, color: LineColor, carriageState: CarriageState
): PlotterState {
  return { position: { ...position }, angle, color, carriageState };
}

const initialState = initializePlotterState(
  { x: 0, y: 0 }, 0, LineColor.BLACK, CarriageState.UP
);

const program = sequence([
  drawTriangle(100),
  setPosition({ x: 10, y: 10 }),
  setColor(LineColor.RED),
  drawSquare(80)
]);
const result = program(initialState);

// Единственная граница побочных эффектов — вывод готового журнала.
const printer: Printer = message => console.log(message);
result.messages.forEach(printer);
