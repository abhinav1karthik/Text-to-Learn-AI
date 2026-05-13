export default function TextInput({ id, label, helperText, ...props }) {
  return (
    <div className="space-y-2">
      <label className="block text-sm font-semibold text-slate-950 dark:text-slate-100" htmlFor={id}>
        {label}
      </label>
      <input
        className="block min-h-12 w-full rounded-lg border border-slate-300 bg-white px-4 py-3 text-base text-slate-950 shadow-sm outline-none transition placeholder:text-slate-400 focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 disabled:cursor-not-allowed disabled:bg-slate-100 disabled:text-slate-500 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100 dark:placeholder:text-slate-500 dark:disabled:bg-slate-900"
        id={id}
        {...props}
      />
      {helperText ? <p className="text-sm leading-6 text-slate-500 dark:text-slate-400">{helperText}</p> : null}
    </div>
  );
}
