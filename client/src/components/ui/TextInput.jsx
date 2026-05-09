export default function TextInput({ id, label, helperText, ...props }) {
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      <input id={id} {...props} />
      {helperText ? <p>{helperText}</p> : null}
    </div>
  );
}
